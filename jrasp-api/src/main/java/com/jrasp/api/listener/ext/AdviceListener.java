package com.jrasp.api.listener.ext;

public class AdviceListener {

    protected void before(Advice advice) throws Throwable {
    }

    protected void afterReturning(Advice advice) throws Throwable {

    }

    protected void after(Advice advice) throws Throwable {
    }

    protected void afterThrowing(Advice advice) throws Throwable {

    }

    protected void beforeCall(Advice advice,
                              int callLineNum,
                              String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {

    }


    protected void afterCallReturning(Advice advice,
                                      int callLineNum,
                                      String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {

    }

    protected void afterCallThrowing(Advice advice,
                                     int callLineNum,
                                     String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc,
                                     String callThrowJavaClassName) {

    }

    protected void afterCall(Advice advice,
                             int callLineNum,
                             String callJavaClassName,
                             String callJavaMethodName, String callJavaMethodDesc, String callThrowJavaClassName) {
    }

    protected void beforeLine(Advice advice, int lineNum) {

    }

}
