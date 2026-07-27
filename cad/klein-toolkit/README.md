# Klein 65200 Mini Ratchet Clip

3D-printable snap cradle for the **Klein Tools 65200** slim-profile mini ratchet.
Designed to drop into a larger toolkit (paired with the Klein 6-in-1 flip socket).

## Files

| File | Use |
|------|-----|
| `klein_65200_ratchet_clip.step` | **Import this into Fusion 360** (solid body, editable) |
| `klein_65200_ratchet_clip.stl` | Slice directly for a test print |
| `klein_65200_ratchet_clip.py` | Parametric source (build123d) — tweak dims and regenerate |

## Open in Fusion 360

1. **Insert → Insert Mesh** is *not* what you want for the STEP.
2. Use **File → Open** (or drag) `klein_65200_ratchet_clip.step`.
3. Fusion creates a solid body. Convert to a component if you are assembling a full kit tray.
4. To loosen/tighten fit: **Press Pull** the pocket faces, or edit parameters in the `.py` and re-export STEP.

## What it does

- **Head cradle** — nests the dual-drive ratchet head
- **Hourglass handle pocket** — matches the tapered 66.51 mm shoulder-to-shoulder span from your caliper photo
- **Two C-snap clips** — grab the narrow mid-handle from above
- **Finger-loop post** — ring drops over a locating nub so the tool cannot slide lengthwise

Insert from the top; pull straight up to remove.

## Measured / assumed dimensions

| Feature | Value | Source |
|---------|-------|--------|
| Handle span (head shoulder → ring shoulder) | **66.51 mm** | Your calipers |
| Head length × width × thickness | 22 × 20 × 12 mm | Typical 65200 (verify) |
| Handle mid width × thickness | 11 × 6.2 mm | Typical (verify) |
| Ring OD / ID | 31 / 21 mm | Typical (verify) |
| XY clearance | 0.45 mm/side | Starting fit |
| Snap lip | 0.75 mm | Starting retention |

### Measure these on your ratchet before a final print

1. Handle **thickness** (flat-to-flat)
2. Handle **width** at the narrow middle
3. Head **thickness** and **width**
4. Ring **outer diameter** and **inner diameter**

Edit the constants at the top of `klein_65200_ratchet_clip.py`, then:

```bash
pip install build123d
python3 klein_65200_ratchet_clip.py
```

## Print settings (starting point)

- Material: **PETG** preferred for living snaps (PLA works for a fit check)
- Orientation: flat on the back (as modeled) — snap arms flex across layers
- Walls: 3+ perimeters
- Infill: 25–40%
- If too tight: increase `XY_CLEAR` / `Z_CLEAR` by 0.1 mm and regenerate
- If too loose: decrease clearance or increase `CLIP_LIP`

## Part size

Approx **125.5 × 39.9 × 17.1 mm**
