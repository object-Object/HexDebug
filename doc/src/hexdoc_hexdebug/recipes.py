from typing import Any

from hexdoc.minecraft.recipe import CraftingShapedRecipe, ItemResult
from pydantic import field_validator


class FocusHolderFillingShapedRecipe(
    CraftingShapedRecipe, type="hexdebug:focus_holder_filling_shaped"
):
    result_inner: ItemResult

    @field_validator("result", mode="before")
    @classmethod
    def _replace_result(cls, value: Any):
        # hack: hexdoc doesn't support predicates, so add a fake model for the filled variant
        result = {"item": "hexdebug:focus_holder/full"}
        match value:
            case {"count": count} | ItemResult(count=count):
                return result | {"count": count}
            case _:
                return result
