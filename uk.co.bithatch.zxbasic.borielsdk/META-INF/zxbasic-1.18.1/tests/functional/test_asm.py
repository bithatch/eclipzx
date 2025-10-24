import os
import sys

import pytest
import test

TEST_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), "asm")


@pytest.mark.parametrize("fname", [os.path.join(TEST_PATH, f) for f in os.listdir(TEST_PATH) if f.endswith(".asm")])
def test_asm(fname):
    options = ["-d", "-e", "/dev/null", fname]
    if os.path.basename(fname).startswith("zxnext_"):
        options.extend(["-O=-N"])

    test.main(options)
    if test.COUNTER == 0:
        return

    sys.stderr.write(
        "Total: %i, Failed: %i (%3.2f%%)\n" % (test.COUNTER, test.FAILED, 100.0 * test.FAILED / float(test.COUNTER))
    )

    assert test.EXIT_CODE == 0, "ASM program test failed"
