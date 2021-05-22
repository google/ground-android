#!/bin/bash
# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Applies credentials and debug keystore stored locally in:
#   gnd/src/debug/.<project-name>.google-services.json    Firebase config
#   gnd/.<project-name>.secrets.properties            Maps API key
#   ~/.android/.<project-name>.debug.keystore             Debug keystore

# Usage:
#  ./use-creds.sh <project-name>
PROJECT="$1"

set -e

# Firebase credentials.
cp -i gnd/src/debug/.${PROJECT}.google-services.json gnd/src/debug/google-services.json

# Maps API key.
cp -i gnd/.${PROJECT}.secrets.properties gnd/src/secrets.properties

# Replace debug keystore.
cp -i ~/.android/.${PROJECT}.debug.keystore ~/.android/debug.keystore

echo "*** Ensure SHA is added to Firebase console and perform a clean build: ***"

keytool -list -alias androiddebugkey -keystore ~/.android/debug.keystore
