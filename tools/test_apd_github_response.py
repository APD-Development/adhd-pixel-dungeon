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

import io
import unittest
import urllib.error
import urllib.request
from unittest.mock import patch
from apd_github_response import json_response

class GitHubResponseTest(unittest.TestCase):
    def request(self):
        return urllib.request.Request("https://api.github.com/repos/owner/private/releases?secret=query-secret",method="POST",headers={"Authorization":"Bearer fake-token"})
    def failure(self, body):
        return urllib.error.HTTPError("https://api.github.com",403,"Forbidden",{"X-GitHub-Request-Id":"ABC:123"},io.BytesIO(body))
    def test_permission_error_keeps_only_bounded_api_message_and_request_id(self):
        failure=self.failure(b'{"message":"Resource not accessible by integration", "unknown":"private body", "token":"fake-token"}')
        with patch("urllib.request.urlopen",side_effect=failure) as send:
            with self.assertRaises(RuntimeError) as caught:json_response(self.request(),60)
        message=str(caught.exception)
        self.assertIn("POST /repos/owner/private/releases failed HTTP 403",message)
        self.assertIn("Resource not accessible by integration",message)
        self.assertIn("request_id=ABC:123",message)
        for secret in ["fake-token","private body","query-secret"]:self.assertNotIn(secret,message)
        self.assertEqual(1,send.call_count)
    def test_non_json_error_body_is_not_dumped(self):
        with patch("urllib.request.urlopen",side_effect=self.failure(b"private html fake-token")):
            with self.assertRaises(RuntimeError) as caught:json_response(self.request(),60)
        self.assertIn("API error body unavailable",str(caught.exception))
        self.assertNotIn("private html",str(caught.exception))
    def test_even_echoed_authorization_is_redacted(self):
        with patch("urllib.request.urlopen",side_effect=self.failure(b'{"message":"Bad Bearer fake-token fake-token"}')):
            with self.assertRaises(RuntimeError) as caught:json_response(self.request(),60)
        self.assertNotIn("fake-token",str(caught.exception))
    def test_success_returns_json_and_closes_response(self):
        response=io.BytesIO(b'{"id":42}')
        with patch("urllib.request.urlopen",return_value=response):self.assertEqual({"id":42},json_response(self.request(),60))
        self.assertTrue(response.closed)
