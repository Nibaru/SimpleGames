# Klein 65200 Mini Ratchet Clip

Shape-accurate snap cradle for the **Klein Tools 65200** slim-profile mini ratchet.

## What changed (fit fix)

The first version used a wrong simplified block (rectangle head + symmetric hourglass + too-long envelope). This rebuild uses:

- Official envelope **110 × 30 × 20 mm**
- Your caliper span **66.51 mm** between shoulders
- A **photo-traced top silhouette** from Klein’s front product shot, scaled to 110 mm
- Circular head + tapered neck (narrow behind head, flares into ring) + finger-loop post
- Two C-snaps on the true narrow neck (~9.1 mm wide)

## Files

| File | Use |
|------|-----|
| `klein_65200_ratchet_clip.step` | Import into Fusion 360 as a solid |
| `klein_65200_ratchet_clip.stl` | Slice for a test print |
| `klein_65200_ratchet_clip.py` | Parametric source — edit dims, regenerate |
| `outline_preview.png` / `slice_preview.png` | Quick shape checks |

## Open in Fusion 360

1. Open / insert `klein_65200_ratchet_clip.step` (not the STL, if you want editable solids).
2. Drop the ratchet into the pocket to check fit.
3. If tight/loose: edit `XY_CLEAR`, `HANDLE_T`, or `HEAD_T` in the `.py` and re-run, **or** Press-Pull pocket faces in Fusion.

```bash
pip install build123d
python3 klein_65200_ratchet_clip.py
```

## Still verify with calipers before a final print

| Measure | Used in model | Measure yours |
|---------|---------------|---------------|
| Overall length | 110 mm | |
| Ring OD | 29.8 mm | |
| Head OD | 20.4 mm | |
| Neck width (narrowest) | ~9.1 mm | |
| Handle thickness | 7.6 mm | |
| Head / thumbwheel height | 16 mm pocket | |

## Print

- PETG preferred for snaps
- Print flat on the back
- 3+ walls, ~30% infill
- Insert from the top; pull up to remove
