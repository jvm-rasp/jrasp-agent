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

package org.jose4j.jwk;

import org.jose4j.keys.EcKeyUtil;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.JsonHelp;

import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Map;

/**
 */
public class EllipticCurveJsonWebKey extends PublicJsonWebKey
{
    public static final String KEY_TYPE = "EC";

    public static final String CURVE_MEMBER_NAME = "crv";

    public static final String X_MEMBER_NAME = "x";
    public static final String Y_MEMBER_NAME = "y";

    public static final String PRIVATE_KEY_MEMBER_NAME = "d";

    private String curveName;

    public EllipticCurveJsonWebKey(ECPublicKey publicKey)
    {
        super(publicKey);
        ECParameterSpec spec = publicKey.getParams();
        EllipticCurve curve = spec.getCurve();
        curveName = EllipticCurves.getName(curve);
    }

    public EllipticCurveJsonWebKey(Map<String, Object> params) throws JoseException
    {
        super(params);

        curveName = JsonHelp.getString(params, CURVE_MEMBER_NAME);
        ECParameterSpec curve = EllipticCurves.getSpec(curveName);

        BigInteger x = getBigIntFromBase64UrlEncodedParam(params, X_MEMBER_NAME);

        BigInteger y =  getBigIntFromBase64UrlEncodedParam(params, Y_MEMBER_NAME);

        EcKeyUtil keyUtil = new EcKeyUtil();
        key = keyUtil.publicKey(x, y, curve);
        checkForBareKeyCertMismatch();

        if (params.containsKey(PRIVATE_KEY_MEMBER_NAME))
        {
            BigInteger d = getBigIntFromBase64UrlEncodedParam(params, PRIVATE_KEY_MEMBER_NAME);
            privateKey = keyUtil.privateKey(d, curve);
        }
    }

    public ECPublicKey getECPublicKey()
    {
        return (ECPublicKey) key;
    }

    public ECPrivateKey getEcPrivateKey()
    {
        return (ECPrivateKey) privateKey;
    }

    public String getKeyType()
    {
        return KEY_TYPE;
    }

    public String getCurveName()
    {
        return curveName;
    }

    private int getCoordinateByteLength()
    {
        ECParameterSpec spec = EllipticCurves.getSpec(getCurveName());
        return (int) Math.ceil(spec.getCurve().getField().getFieldSize() / 8d);
    }

    protected void fillPublicTypeSpecificParams(Map<String,Object> params)
    {
        ECPublicKey ecPublicKey = getECPublicKey();
        ECPoint w = ecPublicKey.getW();
        int coordinateByteLength = getCoordinateByteLength();
        putBigIntAsBase64UrlEncodedParam(params, X_MEMBER_NAME, w.getAffineX(), coordinateByteLength);
        putBigIntAsBase64UrlEncodedParam(params, Y_MEMBER_NAME, w.getAffineY(), coordinateByteLength);
        params.put(CURVE_MEMBER_NAME, getCurveName());
    }

    protected void fillPrivateTypeSpecificParams(Map<String,Object> params)
    {
        ECPrivateKey ecPrivateKey = getEcPrivateKey();
        
        if (ecPrivateKey != null)
        {
            // This should be 'length of this octet
            // string MUST be ceiling(log-base-2(n)/8) octets (where n is the order
            //  of the curve).' but this is the same thing for the prime integer curves
            int coordinateByteLength = getCoordinateByteLength();
        	putBigIntAsBase64UrlEncodedParam(params, PRIVATE_KEY_MEMBER_NAME, ecPrivateKey.getS(), coordinateByteLength);
        }
    }
}
