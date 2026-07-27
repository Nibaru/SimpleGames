"""
Klein Tools 65200 mini ratchet clip / cradle for 3D printing.

Measured from user photo:
  - Handle span between head shoulder and ring shoulder: 66.51 mm

Other dims are typical 65200 slim-profile proportions, exposed as
parameters at the top so you can tune fit after a test print.

Exports:
  - klein_65200_ratchet_clip.step  (import into Fusion 360)
  - klein_65200_ratchet_clip.stl   (slice directly)
"""

from __future__ import annotations

from pathlib import Path

from build123d import (
    Axis,
    BuildLine,
    BuildPart,
    BuildSketch,
    Circle,
    Locations,
    Mode,
    Plane,
    Polygon,
    Polyline,
    Rectangle,
    export_step,
    export_stl,
    extrude,
    fillet,
    make_face,
)


# ---------------------------------------------------------------------------
# Tunable parameters (mm)
# ---------------------------------------------------------------------------

# From calipers in your photo (shoulder-to-shoulder flat handle)
HANDLE_SPAN = 66.51

# Approximate tool proportions (adjust after measuring your ratchet)
HEAD_LEN = 22.0  # along tool axis
HEAD_W = 20.0  # across tool
HEAD_T = 12.0  # dual-drive head thickness

HANDLE_W_MID = 11.0  # narrowest mid-handle (clip here)
HANDLE_W_END = 12.5  # near shoulders
HANDLE_T = 6.2  # flat handle thickness

RING_OD = 31.0
RING_ID = 21.0
RING_T = 6.2

# Fit / print
XY_CLEAR = 0.45  # per-side clearance in XY
Z_CLEAR = 0.35  # pocket depth clearance
FLOOR = 2.0  # base thickness under tool
CLIP_W = 10.0  # width of each snap arm along handle axis
CLIP_LIP = 0.75  # inward snap lip
CLIP_OPEN_FRAC = 0.78  # mouth opening as fraction of handle thickness
CLIP_ARM = 2.0  # snap arm thickness
EDGE_FILLET = 1.0

# Overall cradle padding beyond tool silhouette
PAD_X = 3.0
PAD_Y = 4.0


def tool_envelope_length() -> float:
    return HEAD_LEN + HANDLE_SPAN + RING_OD


def handle_polygon_points() -> list[tuple[float, float]]:
    """Hourglass-ish handle outline (top view), closed."""
    half_span = HANDLE_SPAN / 2 + XY_CLEAR
    w_end = HANDLE_W_END / 2 + XY_CLEAR
    w_mid = HANDLE_W_MID / 2 + XY_CLEAR
    return [
        (-half_span, -w_end),
        (-half_span * 0.35, -w_mid),
        (half_span * 0.35, -w_mid),
        (half_span, -w_end),
        (half_span, w_end),
        (half_span * 0.35, w_mid),
        (-half_span * 0.35, w_mid),
        (-half_span, w_end),
    ]


def build_clip():
    """Build a top-insert snap cradle for the Klein 65200."""

    tool_len = tool_envelope_length()
    body_len = tool_len + 2 * PAD_X
    body_w = max(HEAD_W, RING_OD) + 2 * PAD_Y + 2 * XY_CLEAR
    pocket_depth = max(HEAD_T, HANDLE_T, RING_T) + Z_CLEAR
    body_h = FLOOR + pocket_depth + CLIP_ARM + CLIP_LIP

    head_center_x = -(HANDLE_SPAN / 2 + HEAD_LEN / 2)
    ring_center_x = HANDLE_SPAN / 2 + RING_OD / 2
    handle_y = HANDLE_W_MID / 2 + XY_CLEAR

    with BuildPart() as part:
        # Outer block
        with BuildSketch(Plane.XY):
            Rectangle(body_len, body_w)
        extrude(amount=body_h)

        cut_h = pocket_depth + CLIP_ARM + CLIP_LIP + 0.2

        # Head pocket
        with BuildSketch(Plane.XY.offset(FLOOR)):
            with Locations((head_center_x, 0)):
                Rectangle(HEAD_LEN + 2 * XY_CLEAR, HEAD_W + 2 * XY_CLEAR)
        extrude(amount=cut_h, mode=Mode.SUBTRACT)

        # Handle pocket (hourglass)
        with BuildSketch(Plane.XY.offset(FLOOR)):
            Polygon(*handle_polygon_points())
        extrude(amount=cut_h, mode=Mode.SUBTRACT)

        # Ring pocket
        with BuildSketch(Plane.XY.offset(FLOOR)):
            with Locations((ring_center_x, 0)):
                Circle(RING_OD / 2 + XY_CLEAR)
        extrude(amount=cut_h, mode=Mode.SUBTRACT)

        # Locating nub through finger loop
        nub_h = min(HANDLE_T, pocket_depth - 0.5)
        with BuildSketch(Plane.XY.offset(FLOOR)):
            with Locations((ring_center_x, 0)):
                Circle(max(2.0, RING_ID / 2 - 2.0))
        extrude(amount=nub_h)

        # Snap arms + lips over mid-handle
        lip_z = FLOOR + HANDLE_T + Z_CLEAR * 0.4
        mouth = HANDLE_T * CLIP_OPEN_FRAC

        for cx in (-HANDLE_SPAN * 0.22, HANDLE_SPAN * 0.22):
            for side in (-1.0, 1.0):
                arm_y = side * (handle_y + CLIP_ARM / 2)
                with BuildSketch(Plane.XY.offset(FLOOR)):
                    with Locations((cx, arm_y)):
                        Rectangle(CLIP_W, CLIP_ARM)
                extrude(amount=HANDLE_T + Z_CLEAR + CLIP_LIP + 0.4)

                lip_y = side * (handle_y - CLIP_LIP / 2)
                with BuildSketch(Plane.XY.offset(lip_z)):
                    with Locations((cx, lip_y)):
                        Rectangle(CLIP_W, CLIP_LIP)
                extrude(amount=CLIP_LIP + 0.5)

            # Keep a snap mouth between the lips
            with BuildSketch(Plane.XY.offset(FLOOR + HANDLE_T * 0.55)):
                with Locations((cx, 0)):
                    Rectangle(CLIP_W + 0.6, mouth)
            extrude(amount=30, mode=Mode.SUBTRACT)

        # Soften vertical outer edges if possible
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
    print(
        "Tool envelope length "
        f"(head+handle+ring): {tool_envelope_length():.2f} mm"
    )
    print(
        "Part bounding box (mm): "
        f"{bb.size.X:.2f} x {bb.size.Y:.2f} x {bb.size.Z:.2f}"
    )


if __name__ == "__main__":
    main()
