#!/usr/bin/env bash
#
# Working Space — ADB smoke test
#
# SPDX-License-Identifier: Apache-2.0
#
# Runs a scripted verification pass against a connected device/emulator to
# confirm the core Working Space subsystems are alive: package install,
# system service, session tracking, focus mode (settings + DND + QS tile +
# notification), and SELinux denial scan.
#
# Usage:
#   adb_smoke_test.sh [serial]
#
# Exit code is non-zero if any REQUIRED check fails. Set -x for verbose adb
# tracing.

set -u

SERIAL="${1:-}"
ADB="adb"
if [[ -n "${SERIAL}" ]]; then
    ADB="adb -s ${SERIAL}"
fi

PKG="com.bmobile.workingspace"
SERVICE_NAME="working_space"
TILE_SPEC="workingspace_focus"
FOCUS_KEY="workingspace_focus_mode_active"
SESSION_KEY="workingspace_session_active"
APP_LIST_KEY="workingspace_app_list"

PASS=0
FAIL=0
FAILED_CHECKS=()

color() { printf '\033[%sm%s\033[0m' "$1" "$2"; }
ok()   { PASS=$((PASS+1)); echo "$(color 32 PASS)  $1"; }
bad()  { FAIL=$((FAIL+1)); FAILED_CHECKS+=("$1"); echo "$(color 31 FAIL)  $1  ${2:-}"; }
info() { echo "$(color 36 INFO)  $1"; }

adbsh() { ${ADB} shell "$@" 2>/dev/null | tr -d '\r'; }

require_device() {
    if ! ${ADB} get-state >/dev/null 2>&1; then
        echo "No device/emulator found on '${SERIAL:-default}'. Aborting." >&2
        exit 2
    fi
}

check_package_installed() {
    local path
    path=$(adbsh pm path "${PKG}")
    if [[ "${path}" == package:* ]]; then
        ok "Package installed (${path#package:})"
    else
        bad "Package installed" "pm path returned: '${path}'"
    fi
}

check_service_registered() {
    local result
    result=$(adbsh service check "${SERVICE_NAME}")
    if [[ "${result}" == *"found"* ]]; then
        ok "System service '${SERVICE_NAME}' registered"
    else
        bad "System service '${SERVICE_NAME}' registered" "service check returned: '${result}'"
    fi
}

check_binder_service_running() {
    local result
    result=$(adbsh dumpsys activity services "${PKG}")
    if [[ "${result}" == *"WorkingSpaceBinderService"* ]]; then
        ok "WorkingSpaceBinderService running"
    else
        bad "WorkingSpaceBinderService running" "not found in dumpsys activity services"
    fi
}

check_settings_key_roundtrip() {
    local namespace="$1" key="$2" value="$3" label="$4"
    adbsh settings put "${namespace}" "${key}" "${value}" >/dev/null
    local got
    got=$(adbsh settings get "${namespace}" "${key}")
    if [[ "${got}" == "${value}" ]]; then
        ok "${label} (${namespace}:${key}=${value})"
    else
        bad "${label}" "expected '${value}', got '${got}'"
    fi
}

check_focus_mode_cycle() {
    info "Toggling focus mode ON via settings…"
    adbsh settings put secure "${FOCUS_KEY}" 1 >/dev/null
    sleep 1

    local focus_val
    focus_val=$(adbsh settings get secure "${FOCUS_KEY}")
    [[ "${focus_val}" == "1" ]] && ok "Focus mode setting = 1" || bad "Focus mode setting = 1" "got '${focus_val}'"

    local zen
    zen=$(adbsh settings get global zen_mode)
    if [[ "${zen}" != "0" ]]; then
        ok "DND engaged while focus mode active (zen_mode=${zen})"
    else
        bad "DND engaged while focus mode active" "zen_mode still 0 (framework may not be applying policy — expected if app-level FocusModeController didn't run in foreground)"
    fi

    local notif
    notif=$(adbsh dumpsys notification --noredact | grep -A 3 "pkg=${PKG}")
    if [[ -n "${notif}" ]]; then
        ok "Focus mode persistent notification present"
    else
        bad "Focus mode persistent notification present" "no active notification found for ${PKG}"
    fi

    info "Toggling focus mode OFF via settings…"
    adbsh settings put secure "${FOCUS_KEY}" 0 >/dev/null
    sleep 1
    focus_val=$(adbsh settings get secure "${FOCUS_KEY}")
    [[ "${focus_val}" == "0" ]] && ok "Focus mode setting = 0 after stop" || bad "Focus mode setting = 0 after stop" "got '${focus_val}'"
}

check_qs_tile_registered() {
    local tiles
    tiles=$(adbsh settings get secure sysui_qs_tiles)
    if [[ "${tiles}" == *"${TILE_SPEC}"* ]]; then
        ok "QS tile '${TILE_SPEC}' present in sysui_qs_tiles"
    else
        info "QS tile '${TILE_SPEC}' not currently added (user-configurable, not a failure): '${tiles}'"
    fi
}

check_session_flag_present() {
    local session
    session=$(adbsh settings get secure "${SESSION_KEY}")
    if [[ "${session}" == "0" || "${session}" == "1" ]]; then
        ok "Session flag readable (${SESSION_KEY}=${session})"
    else
        bad "Session flag readable" "unexpected value '${session}' (null means WorkingSpaceService never wrote it — launch a listed app first)"
    fi
}

check_app_list_key() {
    local list
    list=$(adbsh settings get system "${APP_LIST_KEY}")
    info "workingspace_app_list = '${list}'"
}

check_selinux_denials() {
    local enforce
    enforce=$(adbsh getenforce)
    if [[ "${enforce}" == "Enforcing" ]]; then
        ok "SELinux is Enforcing"
    else
        bad "SELinux is Enforcing" "getenforce returned '${enforce}'"
    fi

    local denials
    denials=$(${ADB} logcat -b events -d 2>/dev/null | grep -i "avc:.*denied" | grep -i "workingspace")
    if [[ -z "${denials}" ]]; then
        ok "No SELinux denials for workingspace in event log"
    else
        bad "No SELinux denials for workingspace in event log" "$(echo "${denials}" | head -5)"
    fi
}

check_no_crash_loop() {
    local crashes
    crashes=$(${ADB} logcat -d 2>/dev/null | grep -i "FATAL EXCEPTION" | grep -i "${PKG}")
    if [[ -z "${crashes}" ]]; then
        ok "No fatal exceptions logged for ${PKG}"
    else
        bad "No fatal exceptions logged for ${PKG}" "$(echo "${crashes}" | head -5)"
    fi
}

main() {
    require_device
    info "Target: $(${ADB} get-state 2>/dev/null) ${SERIAL:+(${SERIAL})}"
    echo

    check_package_installed
    check_service_registered
    check_binder_service_running
    check_session_flag_present
    check_app_list_key
    check_focus_mode_cycle
    check_qs_tile_registered
    check_selinux_denials
    check_no_crash_loop

    echo
    info "Results: ${PASS} passed, ${FAIL} failed"
    if [[ ${FAIL} -gt 0 ]]; then
        echo "Failed checks:"
        for c in "${FAILED_CHECKS[@]}"; do
            echo "  - ${c}"
        done
        exit 1
    fi
    exit 0
}

main "$@"
