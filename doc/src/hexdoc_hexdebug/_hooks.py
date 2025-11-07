import logging
import shutil
from importlib.resources import Package
from pathlib import Path

from hexdoc.cli.app import LoadedBookInfo
from hexdoc.core import Properties
from hexdoc.plugin import (
    HookReturn,
    LoadTaggedUnionsImpl,
    ModPlugin,
    ModPluginImpl,
    ModPluginWithBook,
    hookimpl,
)
from jinja2.sandbox import SandboxedEnvironment
from typing_extensions import override

import hexdoc_hexdebug

from . import recipes
from .__gradle_version__ import FULL_VERSION, MINECRAFT_VERSION, MOD_ID, MOD_VERSION
from .__version__ import PY_VERSION

logger = logging.getLogger(__name__)


class HexDebugPlugin(LoadTaggedUnionsImpl, ModPluginImpl):
    @staticmethod
    @hookimpl
    def hexdoc_load_tagged_unions() -> HookReturn[Package]:
        return [recipes]

    @staticmethod
    @hookimpl
    def hexdoc_mod_plugin(branch: str) -> ModPlugin:
        return HexDebugModPlugin(branch=branch)


class HexDebugModPlugin(ModPluginWithBook):
    @property
    @override
    def modid(self) -> str:
        return MOD_ID

    @property
    @override
    def full_version(self) -> str:
        return FULL_VERSION

    @property
    @override
    def mod_version(self) -> str:
        return f"{MOD_VERSION}+{MINECRAFT_VERSION}"

    @property
    @override
    def plugin_version(self) -> str:
        return PY_VERSION

    @override
    def resource_dirs(self) -> HookReturn[Package]:
        # lazy import because generated may not exist when this file is loaded
        # eg. when generating the contents of generated
        # so we only want to import it if we actually need it
        from ._export import generated

        return generated

    @override
    def jinja_template_root(self) -> tuple[Package, str]:
        return hexdoc_hexdebug, "_templates"

    @override
    def pre_render_site(
        self,
        props: Properties,
        books: list[LoadedBookInfo],
        env: SandboxedEnvironment,
        output_dir: Path,
    ) -> None:
        if props.modid == self.modid:
            src = Path("build/dokka/html").resolve()
            dst = output_dir / "api"
            logger.info(f"Copying Dokka site from {src} to {dst}.")
            shutil.copytree(src, dst, dirs_exist_ok=True)
