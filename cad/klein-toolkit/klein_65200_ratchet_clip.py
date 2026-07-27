"""
Klein Tools 65200 mini ratchet snap cradle — shape-accurate rebuild.

Official envelope: 110 x 30 x 20 mm (L x W x H)
User caliper: 66.51 mm shoulder-to-shoulder handle span

Top view is rebuilt from Klein's front product photo scaled to 110 mm:
  circular head + continuous tapered neck/flare + circular finger loop.

Exports STEP (Fusion) + STL (print).
"""

from __future__ import annotations

from pathlib import Path

from build123d import (
    Axis,
    BuildPart,
    BuildSketch,
    Circle,
    Locations,
    Mode,
    Plane,
    Polygon,
    Rectangle,
    export_step,
    export_stl,
    extrude,
    fillet,
)


# ---------------------------------------------------------------------------
# Known / measured (mm)
# ---------------------------------------------------------------------------

OVERALL_LEN = 110.0
OVERALL_W = 29.8

HEAD_OD = 20.4
HEAD_CENTER_FROM_TIP = 10.0
RING_OD = 29.8
RING_ID = 18.5
RING_CENTER_FROM_TIP = 94.0

HANDLE_T = 7.6
HEAD_T = 16.0

XY_CLEAR = 0.55
Z_CLEAR = 0.40
FLOOR = 2.2
WALL = 3.2
CLIP_W = 9.0
CLIP_LIP = 0.85
CLIP_ARM = 2.2
CLIP_OPEN_FRAC = 0.80
EDGE_FILLET = 1.0

# Half-widths from tip (mm), Klein front photo scaled to 110 mm overall length.
# Covers head bulge through neck and flare into the ring so circles fuse cleanly.
HALF_W_FROM_TIP: list[tuple[float, float]] = [
    (0.0, 2.10),
    (2.0, 6.36),
    (4.0, 7.94),
    (6.0, 8.89),
    (8.0, 9.85),
    (10.0, 10.13),
    (12.0, 9.96),
    (14.0, 9.23),
    (16.0, 7.94),
    (18.0, 6.70),
    (20.0, 5.74),
    (22.0, 5.29),
    (24.0, 4.95),
    (26.0, 4.73),
    (28.0, 4.67),
    (30.0, 4.62),
    (32.0, 4.56),
    (34.0, 4.56),
    (36.0, 4.56),
    (38.0, 4.56),
    (40.0, 4.56),
    (42.0, 4.56),
    (44.0, 4.56),
    (46.0, 4.62),
    (48.0, 4.67),
    (50.0, 4.79),
    (52.0, 4.95),
    (54.0, 5.07),
    (56.0, 5.29),
    (58.0, 5.52),
    (60.0, 5.80),
    (62.0, 6.08),
    (64.0, 6.42),
    (66.0, 6.81),
    (68.0, 7.26),
    (70.0, 7.71),
    (72.0, 8.16),
    (74.0, 8.67),
    (76.0, 9.18),
    (78.0, 9.68),
    (80.0, 10.25),
    (82.0, 10.81),
    (84.0, 11.54),
    (86.0, 12.27),
    (88.0, 13.12),
    (90.0, 13.85),
    (92.0, 14.47),
    (94.0, 14.75),
    (96.0, 14.75),
    (98.0, 14.52),
    (100.0, 13.96),
    (102.0, 13.06),
    (104.0, 11.77),
    (106.0, 10.08),
    (108.0, 7.32),
    (110.0, 2.00),
]


def tip_to_centered(x_from_tip: float) -> float:
    return x_from_tip - OVERALL_LEN / 2.0


def half_width_at(x_from_tip: float) -> float:
    tab = HALF_W_FROM_TIP
    if x_from_tip <= tab[0][0]:
        return tab[0][1]
    if x_from_tip >= tab[-1][0]:
        return tab[-1][1]
    for (x0, w0), (x1, w1) in zip(tab, tab[1:]):
        if x0 <= x_from_tip <= x1:
            t = (x_from_tip - x0) / (x1 - x0)
            return w0 + t * (w1 - w0)
    return tab[-1][1]


def silhouette_polygon(clear: float) -> list[tuple[float, float]]:
    """Closed CCW top-view outline of tool + clearance."""
    upper = [
        (tip_to_centered(x), half_width_at(x) + clear) for x, _ in HALF_W_FROM_TIP
    ]
    lower = [
        (tip_to_centered(x), -(half_width_at(x) + clear))
        for x, _ in reversed(HALF_W_FROM_TIP)
    ]
    return upper + lower


def build_clip():
    clear = XY_CLEAR
    outline = silhouette_polygon(clear)

    xs = [p[0] for p in outline]
    ys = [p[1] for p in outline]
    body_len = (max(xs) - min(xs)) + 2 * WALL
    body_w = (max(ys) - min(ys)) + 2 * WALL
    pocket_depth = max(HEAD_T, HANDLE_T) + Z_CLEAR
    body_h = FLOOR + pocket_depth + CLIP_ARM + CLIP_LIP

    ring_c = tip_to_centered(RING_CENTER_FROM_TIP)
    head_c = tip_to_centered(HEAD_CENTER_FROM_TIP)

    # Clip on the true narrow neck (~9.1 mm wide), just behind the head
    clip_xs = [tip_to_centered(30.0), tip_to_centered(42.0)]
    neck_half = half_width_at(36.0) + clear

    with BuildPart() as part:
        with BuildSketch(Plane.XY):
            Rectangle(body_len, body_w)
        extrude(amount=body_h)

        cut_h = pocket_depth + CLIP_ARM + CLIP_LIP + 0.5

        # Continuous silhouette pocket (photo-derived)
        with BuildSketch(Plane.XY.offset(FLOOR)):
            Polygon(*outline)
            # Reinforce circular ends so faceting cannot undersize them
            with Locations((head_c, 0)):
                Circle(HEAD_OD / 2 + clear)
            with Locations((ring_c, 0)):
                Circle(RING_OD / 2 + clear)
        extrude(amount=cut_h, mode=Mode.SUBTRACT)

        # Finger-loop locating post
        nub_r = max(2.5, RING_ID / 2.0 - 1.4)
        with BuildSketch(Plane.XY.offset(FLOOR)):
            with Locations((ring_c, 0)):
                Circle(nub_r)
        extrude(amount=HANDLE_T * 0.85)

        # C-snaps over narrow neck
        lip_z = FLOOR + HANDLE_T + Z_CLEAR * 0.35
        mouth = HANDLE_T * CLIP_OPEN_FRAC

        for cx in clip_xs:
            for side in (-1.0, 1.0):
                arm_y = side * (neck_half + CLIP_ARM / 2)
                with BuildSketch(Plane.XY.offset(FLOOR)):
                    with Locations((cx, arm_y)):
                        Rectangle(CLIP_W, CLIP_ARM)
                extrude(amount=HANDLE_T + Z_CLEAR + CLIP_LIP + 0.5)

                lip_y = side * (neck_half - CLIP_LIP / 2)
                with BuildSketch(Plane.XY.offset(lip_z)):
                    with Locations((cx, lip_y)):
                        Rectangle(CLIP_W * 0.9, CLIP_LIP)
                extrude(amount=CLIP_LIP + 0.45)

            with BuildSketch(Plane.XY.offset(FLOOR + HANDLE_T * 0.5)):
                with Locations((cx, 0)):
                    Rectangle(CLIP_W + 0.8, mouth)
            extrude(amount=40, mode=Mode.SUBTRACT)

        # Finger scoop at ring side
        with BuildSketch(Plane.XY.offset(FLOOR)):
            with Locations((ring_c, body_w / 2 - 1.0)):
                Circle(7.5)
        extrude(amount=cut_h, mode=Mode.SUBTRACT)

        try:
            vertical = part.edges().filter_by(Axis.Z)
            if len(vertical) > 0:
                fillet(vertical, EDGE_FILLET)
        except Exception:
            pass

    return part.part


def main() -> None:
    out_dir = Path(__file__).resolve().parent
    model = build_clip()

    step_path = out_dir / "klein_65200_ratchet_clip.step"
    stl_path = out_dir / "klein_65200_ratchet_clip.stl"
    export_step(model, step_path)
    export_stl(model, stl_path)

    bb = model.bounding_box()
    print(f"Wrote {step_path}")
    print(f"Wrote {stl_path}")
    print(f"Bounding box (mm): {bb.size.X:.2f} x {bb.size.Y:.2f} x {bb.size.Z:.2f}")
    print(f"Volume: {model.volume:.0f} mm^3")


if __name__ == "__main__":
    main()
