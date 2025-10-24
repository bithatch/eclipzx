import os
import sys

import pytest
import test

TEST_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), "zxbpp")


@pytest.mark.parametrize("fname", [os.path.join(TEST_PATH, f) for f in os.listdir(TEST_PATH) if f.endswith(".bi")])
def test_prepro(fname):
    test.main(["-d", "-e", "/dev/null", fname])
    if test.COUNTER == 0:
        return
    sys.stderr.write(
        "Total: %i, Failed: %i (%3.2f%%)\n" % (test.COUNTER, test.FAILED, 100.0 * test.FAILED / float(test.COUNTER))
    )

    assert test.EXIT_CODE == 0, "Preprocessor test failed"
