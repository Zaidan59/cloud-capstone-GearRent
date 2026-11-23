import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path(__file__).parents[1] / 'scripts' / 'capacity.py'
spec = importlib.util.spec_from_file_location('capacity', SCRIPT)
capacity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capacity)


class MeasurementMathTest(unittest.TestCase):
    def test_cpu_units_are_normalized_to_millicores(self):
        self.assertAlmostEqual(capacity.cpu_millicores('25000000n'), 25)
        self.assertAlmostEqual(capacity.cpu_millicores('12500u'), 12.5)
        self.assertAlmostEqual(capacity.cpu_millicores('100m'), 100)
        self.assertAlmostEqual(capacity.cpu_millicores('0.5'), 500)

    def test_memory_binary_and_decimal_units(self):
        self.assertEqual(capacity.memory_mib('192Mi'), 192)
        self.assertEqual(capacity.memory_mib('1Gi'), 1024)
        self.assertEqual(capacity.memory_mib('1048576'), 1)
        self.assertAlmostEqual(capacity.memory_mib('100M'), 100000000 / 1048576)

    def test_nearest_rank_percentile(self):
        self.assertIsNone(capacity.percentile([], 95))
        self.assertEqual(capacity.percentile(range(1, 101), 95), 95)
        self.assertEqual(capacity.percentile([7], 95), 7)

    def test_cost_is_per_successful_request(self):
        model = {'hours_per_month': 720, 'monthly_successful_requests': 100000,
                 'vcpu_hour_idr': 1000, 'gib_hour_idr': 100,
                 'storage_gib_month_idr': 500, 'storage_gib': 20,
                 'vm_vcpu': 2, 'vm_memory_gib': 4, 'fixed_monthly_idr': 0}
        result = capacity.cost_model(model)
        self.assertEqual(result['monthly_total_idr'], 1738000)
        self.assertEqual(result['idr_per_1000_successful_requests'], 17380)
        model['monthly_successful_requests'] = 0
        with self.assertRaises(ValueError):
            capacity.cost_model(model)


if __name__ == '__main__':
    unittest.main()
