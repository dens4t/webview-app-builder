#!/usr/bin/env python3
import html
import os
import pathlib
import re
import sys
from urllib.parse import urlparse


def required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        sys.exit(f"missing {name}")
    return value


def main() -> None:
    url = required("APP_URL")
    if len(url) > 2048:
        sys.exit("url too long")
    if any(ch in url for ch in "\r\n\t\"<>"):
        sys.exit("invalid url characters")

    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        sys.exit("url must start with http:// or https://")

    name = os.environ.get("APP_NAME", "").strip() or (parsed.hostname or "WebView App")
    name = re.sub(r"[\x00-\x1f\x7f<>]", "", name).strip()[:40] or "WebView App"

    job_id = required("APP_ID")
    suffix = re.sub(r"[^a-z0-9]", "", job_id.lower()) or "app"
    if not suffix[0].isalpha():
        suffix = "a" + suffix
    application_id = "com.webviewbuilder.app." + suffix
    show_ads = os.environ.get("SHOW_ADS", "1").strip() != "0"
    ad_url = os.environ.get("APP_AD_URL", "").strip() or "https://builder.deni.ai.id"
    if not (ad_url.startswith("https://") or ad_url.startswith("http://")):
        ad_url = "https://builder.deni.ai.id"

    resources = pathlib.Path("app/src/main/res/values")
    resources.mkdir(parents=True, exist_ok=True)
    (resources / "config.xml").write_text(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        "<resources>\n"
        f"    <string name=\"app_name\">{html.escape(name)}</string>\n"
        f"    <string name=\"app_url\">{html.escape(url)}</string>\n"
        f"    <string name=\"ad_url\">{html.escape(ad_url)}</string>\n"
        f"    <bool name=\"show_ads\">{'true' if show_ads else 'false'}</bool>\n"
        "</resources>\n",
        encoding="utf-8",
    )

    gradle = pathlib.Path("app/build.gradle.kts")
    text = gradle.read_text(encoding="utf-8")
    text = re.sub(r'applicationId = "[^"]*"', f'applicationId = "{application_id}"', text)
    gradle.write_text(text, encoding="utf-8")

    print(f"configured {application_id} -> {url}")


if __name__ == "__main__":
    main()
