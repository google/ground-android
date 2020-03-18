# Copyright 2020 Google LLC
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

"""Generates a SVG by parsing json report.

Task dependencyUpdates generates a json report. This script parses that file and
generates a SVG showing percentage of up-to-date dependencies.

Usage:
> python cloud-builder/generate_dependency_health_svg.py <path_to_json_report> <output_svg_path>
"""

import json
from sys import argv

SVG_TEMPLATE = 'cloud-builder/dependency.svg'

COLOR_BAD = '#e81717'
COLOR_POOR = '#e89417'
COLOR_FAIR = '#d8e817'
COLOR_GOOD = '#8ae817'
COLOR_EXCELLENT = '#1F8F16'


def get_status_color(health):
    if health > 80:
        return COLOR_EXCELLENT
    elif 60 < health <= 80:
        return COLOR_GOOD
    elif 40 < health <= 60:
        return COLOR_FAIR
    elif 20 < health <= 40:
        return COLOR_POOR
    else:
        return COLOR_BAD


def create_svg(output_path, health):
    """Create a new svg from template by replacing placeholders."""
    with open(SVG_TEMPLATE, 'r') as template:
        with open(output_path, 'w') as dest:
            data = template.read()

            status_text = '{}%'.format(health)
            data = data.replace('_%_', status_text)

            color = get_status_color(health)
            data = data.replace('_color_', color)

            dest.write(data)


def print_stats(data):
    # Total detected gradle dependencies
    total = data['count']
    del data['count']

    # Gradle version info
    running_gradle = data['gradle']['running']['version']
    latest_gradle = data['gradle']['current']['version']
    del data['gradle']

    if running_gradle != latest_gradle:
        print('-------------------------')
        print('Gradle: {} -> {}'.format(running_gradle, latest_gradle))

    # Dependencies status
    print('-------------------------')
    for k in data:
        print '{:10} : {:10}'.format(k, data[k].get('count'))
    print('-------------------------')
    print('Total = {}'.format(total))
    print('-------------------------')


def calculate_dependency_health(json_report):
    """Parses json report and calculates percentage of up-to-date dependencies."""
    with open(json_report) as f:
        data = json.load(f)

        print_stats(data.copy())

        health = 100 * data.get('current').get('count') / data['count']
        print('Healthy percentage : {}%'.format(health))

        return health


def main():
    if len(argv) != 3:
        raise Exception('Need exactly 2 arguments: <json_report> <output_path>')

    create_svg(argv[2], calculate_dependency_health(argv[1]))


if __name__ == "__main__":
    main()
