#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""A virtual USB mouse on an Android device, for testing Mouse Lab and making the README's screenshots.

    tools/vmouse.py circles SECONDS TICK_MS [esc|right]   move in circles, one report every TICK_MS
    tools/vmouse.py swipes ANGLE COUNT [esc|right]          COUNT identical swipes tilted ANGLE degrees
    tools/vmouse.py hand ANGLE COUNT [esc|right]            COUNT hand-like swipes around ANGLE degrees
    tools/vmouse.py park                                    move the pointer to the right edge of the screen

It registers a mouse through Android's `uinput` shell tool over adb (`adb shell uinput -`) and
times every report from this machine. ANGLE is clockwise of horizontal, as on screen, so Mouse Lab
should recommend about -ANGLE. Click Mouse Lab's box first so the pointer is captured; esc or
right ends the measurement with that key or button.
"""
import json
import math
import os
import random
import subprocess
import sys
import time

ADB = [os.environ.get("ADB", os.path.expanduser("~/android-sdk/platform-tools/adb"))]
EV_SYN, EV_KEY, EV_REL = 0, 1, 2
REL_X, REL_Y = 0, 1
BTN_LEFT, BTN_RIGHT, KEY_ESC = 272, 273, 1


class Mouse:
    def __init__(self):
        self.p = subprocess.Popen(ADB + ["shell", "uinput", "-"], stdin=subprocess.PIPE, text=True)
        self.send({"id": 1, "command": "register", "name": "Mouse Lab test mouse", "vid": 6353, "pid": 30002,
                   "bus": "usb", "configuration": [
                       {"type": 100, "data": [EV_KEY, EV_REL]},
                       {"type": 101, "data": [BTN_LEFT, BTN_RIGHT, KEY_ESC]},
                       {"type": 102, "data": [REL_X, REL_Y]}]})
        time.sleep(1.5)
        self.ev(EV_REL, REL_X, 0)  # the first event after registering arrives seconds late
        time.sleep(3.5)
        self.next = time.time()

    def send(self, cmd):
        self.p.stdin.write(json.dumps(cmd) + "\n")
        self.p.stdin.flush()

    def ev(self, *events):
        self.send({"id": 1, "command": "inject", "events": list(events) + [EV_SYN, 0, 0]})

    def wait(self, ms):
        """Paces reports from here: uinput's own delay command plays what follows in one burst."""
        self.next = max(self.next, time.time() - 0.05) + ms / 1000
        time.sleep(max(0.0, self.next - time.time()))

    def press(self, code):
        self.ev(EV_KEY, code, 1)
        time.sleep(0.05)
        self.ev(EV_KEY, code, 0)

    def move(self, dx, dy, step, tick_ms):
        """One straight stroke of (dx, dy) counts in whole-count reports of about `step` counts."""
        n = max(1, round(math.hypot(dx, dy) / step))
        sx = sy = 0
        for i in range(1, n + 1):
            tx, ty = round(dx * i / n), round(dy * i / n)
            self.ev(EV_REL, REL_X, tx - sx, EV_REL, REL_Y, ty - sy)
            sx, sy = tx, ty
            self.wait(tick_ms)

    def swipes(self, angle, count, length=300, hand=False):
        """Alternating swipes. A hand swings between two rough end points around where it started."""
        rng = random.Random(7)
        x = 0.0
        for i in range(count):
            a = math.radians(angle + (rng.gauss(0, 1.4) if hand else 0))
            s = 1 if i % 2 == 0 else -1
            if hand:
                dx = s * length / 2 * rng.uniform(0.8, 1.15) - x
            else:
                dx = s * length * math.cos(a)
            dy = dx * math.tan(a)
            self.move(dx, dy, 10, 4)
            x += dx
            if hand:  # the hand drifts a little up or down between swipes
                self.move(0, rng.choice([-1, 1]) * rng.randint(3, 10), 2, 4)

    def circles(self, seconds, tick_ms):
        t0 = time.time()
        k = 0
        while time.time() - t0 < seconds:
            a = k * 0.2
            self.ev(EV_REL, REL_X, round(6 * math.cos(a)), EV_REL, REL_Y, round(6 * math.sin(a)))
            k += 1
            self.wait(tick_ms)
        return k

    def close(self):
        self.p.stdin.close()
        self.p.wait(timeout=10)


def main():
    m = Mouse()
    cmd = sys.argv[1]
    if cmd == "circles":
        print("sent", m.circles(float(sys.argv[2]), float(sys.argv[3])), "reports")
    elif cmd == "park":  # move the visible pointer to the right edge of the screen
        m.move(4000, 0, 40, 4)
    elif cmd in ("swipes", "hand"):
        m.swipes(float(sys.argv[2]), int(sys.argv[3]), length=500 if cmd == "hand" else 300, hand=cmd == "hand")
    time.sleep(0.5)
    if "esc" in sys.argv:
        m.press(KEY_ESC)
    if "right" in sys.argv:
        m.press(BTN_RIGHT)
    time.sleep(0.5)
    m.close()


if __name__ == "__main__":
    main()
