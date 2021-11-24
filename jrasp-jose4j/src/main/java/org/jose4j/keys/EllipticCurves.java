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

import java.math.BigInteger;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.HashMap;
import java.util.Map;

/**
 * Values for these curve parameters taken from FIPS PUB 186-3
 * and http://www.nsa.gov/ia/_files/nist-routines.pdf
 */
public class EllipticCurves
{

    public static final String P_256 = "P-256";
    public static final String P_384 = "P-384";
    public static final String P_521 = "P-521";

    private static final Map<String, ECParameterSpec> nameToSpec = new HashMap<String, ECParameterSpec>();
    private static final Map<EllipticCurve, String> curveToName = new HashMap<EllipticCurve, String>();

    private static void addCurve(String name, ECParameterSpec spec)
    {
        nameToSpec.put(name, spec);
        curveToName.put(spec.getCurve(), name);
    }

    public static ECParameterSpec getSpec(String name)
    {
        return nameToSpec.get(name);
    }

    public static String getName(EllipticCurve curve)
    {
        // equals and hashcode are defined on EllipticCurve so this works
        return curveToName.get(curve);
    }

    // cofactor h (Thus, for these curves over prime fileds, the cofactor is always h = 1)
    private static final int COFACTOR = 1;

    public static final ECParameterSpec P256 = new ECParameterSpec(
        new EllipticCurve(
            // field the finite field that this elliptic curve is over.
            new ECFieldFp(new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951")),
            // a the first coefficient of this elliptic curve.
            new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853948"),
            // b the second coefficient of this elliptic curve.
            new BigInteger("41058363725152142129326129780047268409114441015993725554835256314039467401291")
        ),
        //g the generator which is also known as the base point.
        new ECPoint(
            // gx
            new BigInteger("48439561293906451759052585252797914202762949526041747995844080717082404635286"),
            // gy
            new BigInteger("36134250956749795798585127919587881956611106672985015071877198253568414405109")
        ),
        // Order n
        new BigInteger("115792089210356248762697446949407573529996955224135760342422259061068512044369"),
        COFACTOR);

    public static final ECParameterSpec P384 = new ECParameterSpec(
        new EllipticCurve(
            // field the finite field that this elliptic curve is over.
            new ECFieldFp(new BigInteger("39402006196394479212279040100143613805079739270465" +
                    "44666794829340424572177149687032904726608825893800" +
                    "1861606973112319")),
            // a the first coefficient of this elliptic curve.
            new BigInteger("39402006196394479212279040100143613805079739270465" +
                    "44666794829340424572177149687032904726608825893800" +
                    "1861606973112316"),
            // b the second coefficient of this elliptic curve.
            new BigInteger("27580193559959705877849011840389048093056905856361" +
                    "56852142870730198868924130986086513626076488374510" +
                    "7765439761230575")
        ),
        //g the generator which is also known as the base point.
        new ECPoint(
            // gx
            new BigInteger("26247035095799689268623156744566981891852923491109" +
                    "21338781561590092551885473805008902238805397571978" +
                    "6650872476732087"),
            // gy
            new BigInteger("83257109614890299855467512895201081792878530488613" +
                    "15594709205902480503199884419224438643760392947333" +
                    "078086511627871")
        ),
        // Order n
        new BigInteger("39402006196394479212279040100143613805079739270465446667946905279627" +
                    "659399113263569398956308152294913554433653942643"),
        COFACTOR);

        public static final ECParameterSpec P521 = new ECParameterSpec(
        new EllipticCurve(
            // field the finite field that this elliptic curve is over.
            new ECFieldFp(new BigInteger("68647976601306097149819007990813932172694353001433" +
                    "05409394463459185543183397656052122559640661454554" +
                    "97729631139148085803712198799971664381257402829111" +
                    "5057151")),
            // a the first coefficient of this elliptic curve.
            new BigInteger("68647976601306097149819007990813932172694353001433" +
                    "05409394463459185543183397656052122559640661454554" +
                    "97729631139148085803712198799971664381257402829111" +
                    "5057148"),
            // b the second coefficient of this elliptic curve.
            new BigInteger("10938490380737342745111123907668055699362075989516" +
                    "83748994586394495953116150735016013708737573759623" +
                    "24859213229670631330943845253159101291214232748847" +
                    "8985984")
        ),
        //g the generator which is also known as the base point.
        new ECPoint(
            // gx
            new BigInteger("26617408020502170632287687167233609607298591687569" +
                    "73147706671368418802944996427808491545080627771902" +
                    "35209424122506555866215711354557091681416163731589" +
                    "5999846"),
            // gy
            new BigInteger("37571800257700204635455072244911836035944551347697" +
                    "62486694567779615544477440556316691234405012945539" +
                    "56214444453728942852258566672919658081012434427757" +
                    "8376784")
        ),
        // Order n
        new BigInteger("68647976601306097149819007990813932172694353001433" +
                "05409394463459185543183397655394245057746333217197" +
                "53296399637136332111386476861244038034037280889270" +
                "7005449"),
        COFACTOR);

    static
    {
        addCurve(P_256, EllipticCurves.P256);
        addCurve(P_384, EllipticCurves.P384);
        addCurve(P_521, EllipticCurves.P521);
    }

}
