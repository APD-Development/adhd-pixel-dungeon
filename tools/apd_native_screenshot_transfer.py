#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Collect native screenshots serially, then verify complete device/local manifests."""
import hashlib
from pathlib import Path
import re
import subprocess
import threading
import time

from validate_android_visuals import validate as validate_png

REMOTE = '/sdcard/Pictures/APD-Test-Visuals'


def require(value, message):
    if not value:
        raise RuntimeError(message)


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def output(args, **kwargs):
    return subprocess.check_output(args, text=True, **kwargs)


def image_name(name):
    # Test output includes Java nested-class '$' and MediaStore duplicate ' (1)' names.
    require(bool(re.fullmatch(r'[A-Za-z0-9_$(). -]+\.png', name)) and '/' not in name
            and '\\' not in name and not name.startswith('.'), 'Unsafe screenshot filename')
    return name


def parse_manifest(text):
    result = {}
    for line in text.splitlines():
        match = re.fullmatch(r'([0-9a-f]{64})  (.+)', line)
        require(match is not None, 'Malformed device screenshot digest')
        name = image_name(match.group(2))
        require(name not in result, 'Duplicate device screenshot name')
        result[name] = match.group(1)
    require(result, 'Empty device screenshot manifest')
    return result


def pull_image(directory, name, digest=None):
    name = image_name(name)
    target = directory / name
    if target.exists() and (digest is None or sha(target) == digest):
        return
    temporary = directory / (name + '.partial')
    try:
        subprocess.run(['adb', 'pull', REMOTE + '/' + name, str(temporary)],
                       stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, timeout=30, check=True)
        validate_png(temporary.read_bytes())
        require(digest is None or sha(temporary) == digest, 'Transferred screenshot digest differs')
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)


class IncrementalImages:
    """One transfer at a time, stopped before mandatory complete manifest synchronization."""
    def __init__(self, directory):
        self.directory = directory
        self.directory.mkdir(parents=True, exist_ok=True)
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self.collect, daemon=True)

    def collect(self):
        while not self.stop.wait(5):
            try:
                names = output(['adb', 'shell', 'ls', '-1', REMOTE],
                               stderr=subprocess.DEVNULL, timeout=15).splitlines()
                for name in names:
                    if self.stop.is_set():
                        break
                    pull_image(self.directory, name)
            except (subprocess.SubprocessError, OSError, RuntimeError, ValueError):
                # A screenshot being written may not be complete yet; mandatory final synchronization verifies it.
                pass

    def close(self):
        self.stop.set()
        self.thread.join(timeout=50)
        require(not self.thread.is_alive(), 'Background screenshot transfer did not stop')


def device_manifest(command):
    """Retry only transient ADB transport errors, never an invalid or changing manifest."""
    for attempt in range(3):
        try:
            return parse_manifest(output(['adb', 'shell', command], timeout=60))
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
            if attempt == 2:
                raise
            time.sleep(1)


def sync_images(directory):
    # The instrumentation has finished, so the remote set and bytes must remain stable.
    command = 'cd ' + REMOTE + ' && sha256sum -- *.png'
    before = device_manifest(command)
    for name, digest in before.items():
        pull_image(directory, name, digest)
    after = device_manifest(command)
    require(before == after, 'Device screenshot files changed during final transfer')
    local = {path.name: sha(path) for path in directory.glob('*.png')}
    require(local == before, 'Complete device/local screenshot manifests differ')
    return before
