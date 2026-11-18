#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>

"""Bounded GitHub HTTP failure evidence; no retry or permission changes."""
import json
import urllib.error
import urllib.parse
import urllib.request


def json_response(request, timeout):
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        message = "API error body unavailable"
        try:
            payload = json.loads(error.read(8192))
            if isinstance(payload, dict) and isinstance(payload.get("message"), str):
                message = payload["message"]
        except (ValueError, OSError, TypeError):
            pass
        authorization = request.get_header("Authorization", "")
        for secret in (authorization, authorization.partition(" ")[2]):
            if secret:
                message = message.replace(secret, "[redacted]")
        message = " ".join(message.split())[:500]
        request_id = " ".join(str(error.headers.get("X-GitHub-Request-Id", "unavailable")).split())[:100]
        path = urllib.parse.urlsplit(request.full_url).path
        raise RuntimeError(f"GitHub {request.get_method()} {path} failed HTTP {error.code}: {message}; request_id={request_id}") from None
