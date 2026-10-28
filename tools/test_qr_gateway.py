"""Scope checks for the public QR gateway; no network or database writes."""
import unittest
from qr_tunnel import allowed
class ScopeTest(unittest.TestCase):
    uid='12345678-1234-1234-1234-123456789abc'
    def test_allowed(self):
        for method,path in [('GET','/local-wallet/pay/'+self.uid),('POST','/local-wallet/api/'+self.uid+'/confirm'),('POST','/local-wallet/api/'+self.uid+'/status'),('GET','/assets/css/utetra.css?v=20261028'),('GET','/local-wallet/health')]:
            with self.subTest(path=path): self.assertTrue(allowed(method,path))
    def test_rejected(self):
        for method,path in [('GET','/'),('GET','/admin'),('POST','/auth/login'),('GET','/orders/1'),('GET','/assets/../application.properties'),('GET','/local-wallet/pay/'+self.uid+'?redirect=/admin'),('GET','https://evil.test/local-wallet/pay/'+self.uid),('POST','/local-wallet/api/'+self.uid+'/confirm?x=1'),('GET','/local-wallet/pay/%2e%2e'),('GET','/assets/css/utetra.css?redirect=https://evil.test')]:
            with self.subTest(path=path): self.assertFalse(allowed(method,path))
if __name__=='__main__':unittest.main()
