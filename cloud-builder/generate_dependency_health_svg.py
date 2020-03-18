"""Generates a SVG by parsing json report.

Task dependencyUpdates generates a json report. This script parses that file and
generates a SVG showing percentage of up-to-date dependencies.

Usage:
> python cloud-builder/generate_dependency_health_svg.py <path_to_json_report> <output_svg_path>
"""

import json
from sys import argv

TEMPLATES_DIR = 'cloud-builder/templates/'
SVG_TEMPLATE_BAD = TEMPLATES_DIR + 'dependency_health_bad.svg'
SVG_TEMPLATE_AVG = TEMPLATES_DIR + 'dependency_health_average.svg'
SVG_TEMPLATE_GOOD = TEMPLATES_DIR + 'dependency_health_good.svg'


def get_template(health):
    """Returns a SVG template based on overall healthy dependency percentage."""
    if health >= 67:
        return SVG_TEMPLATE_GOOD
    elif 33 < health < 67:
        return SVG_TEMPLATE_AVG
    else:
        return SVG_TEMPLATE_BAD


def create_svg(svg_template, output_path, health):
    """Create a new svg from template and replace placeholder _%_ with health."""
    with open(svg_template, 'r') as template:
        with open(output_path, 'w') as dest:
            data = template.read().replace('_%_', '{}%'.format(health))
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

    input = argv[1]
    output = argv[2]

    health = calculate_dependency_health(input)
    template = get_template(health)
    create_svg(template, output, health)


if __name__ == "__main__":
    main()
