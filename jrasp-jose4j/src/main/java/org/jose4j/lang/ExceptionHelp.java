package org.jose4j.lang;

/**
 *
 */
public class ExceptionHelp
{
    public static String toStringWithCauses(Throwable t)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(t);
        while (t.getCause() != null)
        {
            t = t.getCause();
            sb.append("; caused by: ").append(t);
        }
        return sb.toString();
    }
}
