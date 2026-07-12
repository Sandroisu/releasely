#!/usr/bin/env bash

set -euo pipefail

: "${GITHUB_ACTION_PATH:?GITHUB_ACTION_PATH is required}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"
: "${RELEASELY_PROJECT_PATH=.}"
: "${RELEASELY_BASE_REF=}"
: "${RELEASELY_FAIL_ON=HIGH}"
: "${RELEASELY_MARKDOWN_REPORT=build/releasely/report.md}"
: "${RELEASELY_JSON_REPORT=build/releasely/report.json}"

normalize_from_workspace() {
  local input_path=$1
  local candidate

  if [[ "$input_path" = /* ]]; then
    candidate=$input_path
  else
    candidate="$GITHUB_WORKSPACE/$input_path"
  fi

  realpath -m -- "$candidate"
}

require_workspace_report_path() {
  local report_path=$1
  local workspace_path=$2

  if [[ "$report_path" != "$workspace_path"/* ]]; then
    echo "Releasely report path must be inside GITHUB_WORKSPACE: $report_path" >&2
    return 1
  fi
}

write_output() {
  local output_name=$1
  local output_value=$2
  local delimiter="RELEASELY_OUTPUT_${RANDOM}_$$"

  {
    printf '%s<<%s\n' "$output_name" "$delimiter"
    printf '%s\n' "$output_value"
    printf '%s\n' "$delimiter"
  } >> "$GITHUB_OUTPUT"
}

workspace_path=$(realpath -m -- "$GITHUB_WORKSPACE")
project_path=$(normalize_from_workspace "$RELEASELY_PROJECT_PATH")
markdown_report_path=$(normalize_from_workspace "$RELEASELY_MARKDOWN_REPORT")
json_report_path=$(normalize_from_workspace "$RELEASELY_JSON_REPORT")

require_workspace_report_path "$markdown_report_path" "$workspace_path"
require_workspace_report_path "$json_report_path" "$workspace_path"

mkdir -p -- "$(dirname -- "$markdown_report_path")"
mkdir -p -- "$(dirname -- "$json_report_path")"

cli_path="$GITHUB_ACTION_PATH/build/install/releasely/bin/releasely"
if [[ ! -x "$cli_path" ]]; then
  echo "Releasely executable was not found after installDist: $cli_path" >&2
  exit 1
fi

cli_arguments=(
  scan
  --path "$project_path"
  --markdown-report "$markdown_report_path"
  --json-report "$json_report_path"
)

if [[ -n "$RELEASELY_BASE_REF" ]]; then
  cli_arguments+=(--base-ref "$RELEASELY_BASE_REF")
fi

if [[ -n "$RELEASELY_FAIL_ON" ]]; then
  cli_arguments+=(--fail-on "$RELEASELY_FAIL_ON")
fi

releasely_exit_code=0
"$cli_path" "${cli_arguments[@]}" || releasely_exit_code=$?

write_output markdown-report-path "$markdown_report_path"
write_output json-report-path "$json_report_path"

exit "$releasely_exit_code"
