#!/usr/bin/env python3
"""Rebuild three plots and machine-readable analysis from actual benchmark CSVs."""
import argparse
import csv
import json
import math
import os
from pathlib import Path
import tempfile

os.environ.setdefault("MPLCONFIGDIR", str(Path(tempfile.gettempdir()) / "daa-matplotlib"))
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
COLORS = dict(zip(ALGORITHMS, ["#a43e31", "#c78e21", "#276aa3", "#188575", "#7f55a0"]))


def load(path):
    with path.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))
    for row in rows:
        for key in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
            row[key] = int(row[key])
        row["time_ms"] = float(row["time_ms"])
        row["operations"] = row["dsu_ops"] + row["edge_scans"] + row["comparisons"]
    return rows


def series(rows, algorithm, density):
    return sorted((r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith("_" + density)), key=lambda r: r["n"])


def slope(rows, key):
    values = [r[key] for r in rows]
    if min(values) <= 0:
        return None
    return float(np.polyfit(np.log10([r["n"] for r in rows]), np.log10(values), 1)[0])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", type=Path, default=Path("results"))
    args = parser.parse_args()
    directory = args.results
    rows = load(directory / "results.csv")
    assert len(rows) == 50, "Expected exactly 50 data rows"
    assert len({(r["algorithm"], r["graph"], r["n"]) for r in rows}) == 50
    plots = directory / "plots"
    plots.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.size": 10, "figure.dpi": 150, "axes.spines.top": False, "axes.spines.right": False})

    fig, axes = plt.subplots(1, 2, figsize=(10.8, 4.0), sharey=True)
    for axis, density in zip(axes, ["sparse", "dense"]):
        for algorithm in ALGORITHMS:
            points = series(rows, algorithm, density)
            # Kahn and DAG relaxation have identical counters; different markers reveal overlap.
            axis.loglog([r["n"] for r in points], [r["operations"] for r in points],
                        color=COLORS[algorithm], marker="s" if algorithm == "Kahn" else "o",
                        linestyle="--" if algorithm == "DagShortestPath" else "-", label=algorithm,
                        markersize=4, markerfacecolor="none" if algorithm == "Kahn" else COLORS[algorithm])
        axis.set(title=f"{density.capitalize()} (m = {'4' if density == 'sparse' else '20'}n)", xlabel="Vertices n")
        axis.grid(True, which="both", alpha=.2)
    axes[0].set_ylabel("Recorded operations\n(scans + comparisons + DSU calls)", fontsize=9)
    axes[0].legend(fontsize=8)
    fig.tight_layout()
    fig.savefig(plots / "operations_vs_n.png")
    plt.close(fig)

    fig, axes = plt.subplots(1, 2, figsize=(10.8, 3.5), sharey=True)
    for axis, density in zip(axes, ["sparse", "dense"]):
        for algorithm in ALGORITHMS:
            points = series(rows, algorithm, density)
            axis.semilogx([r["n"] for r in points], [r["edge_scans"] / r["m"] for r in points],
                          color=COLORS[algorithm], marker="s" if algorithm == "Kahn" else "o",
                          linestyle="--" if algorithm in ["Prim", "DagShortestPath"] else "-",
                          markerfacecolor="none" if algorithm in ["Kahn", "Kosaraju"] else COLORS[algorithm],
                          label=algorithm, markersize=5)
        axis.set(title=density.capitalize(), xlabel="Vertices n", ylim=(0, 2.15))
        axis.grid(True, alpha=.2)
    axes[0].set_ylabel("edgeScans / m")
    axes[0].legend(fontsize=7, ncol=2, loc="upper center", bbox_to_anchor=(.5, .88))
    axes[1].text(.04, .62, "Prim = Kosaraju = 2\nKahn = DAG relaxation = 1", transform=axes[1].transAxes, fontsize=9)
    fig.tight_layout()
    fig.savefig(plots / "scans_per_edge.png")
    plt.close(fig)

    fig, axis = plt.subplots(figsize=(8.2, 3.2))
    for density, color in [("sparse", "#276aa3"), ("dense", "#a43e31")]:
        points = series(rows, "Kruskal", density)
        axis.semilogx([r["n"] for r in points], [r["edge_scans"] / r["m"] for r in points],
                      marker="o", label=f"{density}: m={'4' if density == 'sparse' else '20'}n", color=color)
    axis.set(xlabel="Vertices n", ylabel="Examined edges / all input edges", ylim=(0, 1.05),
             title="Kruskal stops scanning after the spanning tree is complete")
    axis.text(.03, .08, "All m edges are still sorted before scanning.", transform=axis.transAxes, fontsize=9)
    axis.grid(True, alpha=.2); axis.legend()
    fig.tight_layout(); fig.savefig(plots / "kruskal_examined_share.png"); plt.close(fig)

    ratios = []
    summary = {"rows": len(rows), "algorithms": {}, "slopes": {}}
    for algorithm in ALGORITHMS:
        subset = [r for r in rows if r["algorithm"] == algorithm]
        summary["algorithms"][algorithm] = {"min_time_ms": min(r["time_ms"] for r in subset),
                                               "max_time_ms": max(r["time_ms"] for r in subset)}
        for density in ["sparse", "dense"]:
            points = series(rows, algorithm, density)
            summary["slopes"][f"{algorithm}_{density}"] = {key: slope(points, key) for key in ["edge_scans", "comparisons", "operations"]}
            for r in points:
                ratios.append({"algorithm": algorithm, "density": density, "n": r["n"], "m": r["m"],
                               "scans_per_edge": r["edge_scans"] / r["m"],
                               "scans_per_v_plus_e": r["edge_scans"] / (r["n"] + r["m"]),
                               "dsu_ops_per_edge": r["dsu_ops"] / r["m"],
                               "comparisons_per_e_log2_e": r["comparisons"] / (r["m"] * math.log2(r["m"]))})
    with (directory / "ratios.csv").open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=list(ratios[0]))
        writer.writeheader(); writer.writerows(ratios)
    bonus = load(directory / "bonus.csv")
    assert len(bonus) == 10
    assert all(r["edge_scans"] == r["m"] for r in bonus)
    summary["tarjan_scans_per_edge"] = 1
    (directory / "analysis.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
