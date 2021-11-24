/*
 * Copyright 2012-2013 Brian Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jose4j.keys;

import junit.framework.TestCase;
import org.jose4j.lang.JoseException;

import java.security.cert.X509Certificate;

/**
 */
public class X509UtilTest extends TestCase
{
    public void testFromBase64Der() throws JoseException
    {
        String s =
                "MIICUTCCAfugAwIBAgIBADANBgkqhkiG9w0BAQQFADBXMQswCQYDVQQGEwJDTjEL\n" +
                "MAkGA1UECBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMC\n" +
                "VU4xFDASBgNVBAMTC0hlcm9uZyBZYW5nMB4XDTA1MDcxNTIxMTk0N1oXDTA1MDgx\n" +
                "NDIxMTk0N1owVzELMAkGA1UEBhMCQ04xCzAJBgNVBAgTAlBOMQswCQYDVQQHEwJD\n" +
                "TjELMAkGA1UEChMCT04xCzAJBgNVBAsTAlVOMRQwEgYDVQQDEwtIZXJvbmcgWWFu\n" +
                "ZzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCp5hnG7ogBhtlynpOS21cBewKE/B7j\n" +
                "V14qeyslnr26xZUsSVko36ZnhiaO/zbMOoRcKK9vEcgMtcLFuQTWDl3RAgMBAAGj\n" +
                "gbEwga4wHQYDVR0OBBYEFFXI70krXeQDxZgbaCQoR4jUDncEMH8GA1UdIwR4MHaA\n" +
                "FFXI70krXeQDxZgbaCQoR4jUDncEoVukWTBXMQswCQYDVQQGEwJDTjELMAkGA1UE\n" +
                "CBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMCVU4xFDAS\n" +
                "BgNVBAMTC0hlcm9uZyBZYW5nggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEE\n" +
                "BQADQQA/ugzBrjjK9jcWnDVfGHlk3icNRq0oV7Ri32z/+HQX67aRfgZu7KWdI+Ju\n" +
                "Wm7DCfrPNGVwFWUQOmsPue9rZBgO\n";

        X509Util x5u = new X509Util();
        X509Certificate x509Certificate = x5u.fromBase64Der(s);
        assertTrue(x509Certificate.getSubjectDN().toString().contains("Yang"));
    }

    public void testFromGoogleEndpoint() throws JoseException
    {
        // took one from https://www.googleapis.com/oauth2/v1/certs
        String bder = "MIICITCCAYqgAwIBAgIINulGhAa6BxUwDQYJKoZIhvcNAQEFBQAwNjE0MDIGA1UE\nAxMrZmVkZXJhdGVkLXNpZ25vbi5zeXN0ZW0uZ3NlcnZpY2VhY2NvdW50LmNvbTAe\nFw0xMzAyMjYwNTI4MzRaFw0xMzAyMjcxODI4MzRaMDYxNDAyBgNVBAMTK2ZlZGVy\nYXRlZC1zaWdub24uc3lzdGVtLmdzZXJ2aWNlYWNjb3VudC5jb20wgZ8wDQYJKoZI\nhvcNAQEBBQADgY0AMIGJAoGBAL9Q8ogQtQfHVzto3p1xiQjBXxcBceE/LTa9jxv4\nEEp0fkKP9bBz/uRlpGkNnP++qkPb6N6s4+mgF12JbTsyRxb4jfXGobfW2lx6HZkX\nRoCk4mAdu3axEVGlYQq0IIsgvNfFiks0Z2pRkovDshPqXBt0FUemM0M7bVODAsZn\ncE3xAgMBAAGjODA2MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMBYGA1Ud\nJQEB/wQMMAoGCCsGAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4GBAA38HHhl0cddqDEd\nswuGUcIvPE1QDqlyfYZUZyZPfZ2JSuYj34DdLm31aq8SOAxNRorpyel/n1bxDUfI\nFueGAkh5AySoPsH7wnj/ZigsidGct9yllIcsqeIvFYkOW53rVwpriU3wcEmh+RzI\nLUYyJkbYf3pY8XHeE56dZqzU+E8Y";
        X509Util x5u = new X509Util();
        X509Certificate x509Certificate = x5u.fromBase64Der(bder);
        assertTrue(x509Certificate.getSubjectDN().toString().contains("federated-signon.system.gserviceaccount.com"));
    }
}
