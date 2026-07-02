"""Install flag block model from Blockbench source JSON (Java Edition)."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = pathlib.Path(__file__).with_name("flag_block_source.json")
TARGET = ROOT / "src/main/resources/assets/faction_control/models/block/flag_block.json"
TEXTURE = "faction_control:block/marcadordebasetextura"
ALLOWED_ROTATIONS = (-45, -22.5, 0, 22.5, 45)


def snap_rotation(angle: float) -> float:
    return min(ALLOWED_ROTATIONS, key=lambda allowed: abs(allowed - angle))


def convert_rotation(rotation: dict) -> dict:
    if "angle" in rotation:
        converted = dict(rotation)
        converted["angle"] = snap_rotation(converted.get("angle", 0))
        return converted

    origin = rotation.get("origin", [8, 8, 8])
    for axis in ("x", "y", "z"):
        angle = rotation.get(axis, 0)
        if angle:
            return {"angle": snap_rotation(angle), "axis": axis, "origin": origin}
    return {"angle": 0, "axis": "y", "origin": origin}


def convert_element(element: dict) -> dict:
    converted = {
        "from": element["from"],
        "to": element["to"],
        "faces": element["faces"],
    }
    if "rotation" in element:
        converted["rotation"] = convert_rotation(element["rotation"])
    return converted


def main() -> int:
    if not SOURCE.exists():
        print(f"Missing {SOURCE}", file=sys.stderr)
        return 1

    src = json.loads(SOURCE.read_text(encoding="utf-8"))
    model = {
        "texture_size": src.get("texture_size", [64, 64]),
        "textures": {
            "1": TEXTURE,
            "particle": TEXTURE,
        },
        "elements": [convert_element(element) for element in src["elements"]],
    }
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(json.dumps(model, indent=2), encoding="utf-8")
    print(f"Wrote {TARGET} ({len(model['elements'])} elements)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
