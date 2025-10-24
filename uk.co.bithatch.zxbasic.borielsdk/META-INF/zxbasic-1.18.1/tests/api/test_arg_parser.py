import unittest

from src.arch.z80 import backend
from src.zxbc.args_parser import parser


class TestArgParser(unittest.TestCase):
    """Test argument options from the cmdline"""

    def setUp(self):
        backend.Backend()  # backend contains "org" option needed in these tests
        self.parser = parser()

    def test_autorun_defaults_to_none(self):
        """Some boolean options, when not specified from the command line
        must return None (null) instead of False to preserve .INI saved
        value.
        """
        options = self.parser.parse_args(["test.bas"])
        self.assertIsNone(options.autorun)

    def test_loader_defaults_to_none(self):
        """Some boolean options, when not specified from the command line
        must return None (null) instead of False to preserve .INI saved
        value.
        """
        options = self.parser.parse_args(["test.bas"])
        self.assertIsNone(options.basic)
