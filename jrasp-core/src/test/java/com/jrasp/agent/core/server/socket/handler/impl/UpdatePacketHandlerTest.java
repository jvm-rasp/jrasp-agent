package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.core.util.FeatureCodec;
import org.junit.Test;

import java.net.URLDecoder;
import java.util.Map;

public class UpdatePacketHandlerTest {

    private static final FeatureCodec kv = new FeatureCodec(';', '=');

    @Test
    public void test() {
        String parameters = "file-algorithm:file_list_action=0;file_read_action=0;file_upload_action=0;danger_dir_list=/,/home,/etc,/usr,/usr/local,/var/log,/proc,/sys,/root,C:\\\\,D:\\\\,E:\\\\;file_upload_black_list=.jsp,.asp,.phar,.phtml,.sh,.py,.pl,.rb;travel_str=../,..\\\\;file_delete_action=0;";
        Map<String, String> parametersMap = kv.toMap(parameters);
        assert parametersMap.size() == 7;
    }

    @Test
    public void testUrlDecode() throws Throwable {
        String html = "%3Chtml%20class=%22no-js%22%20style=%22background-color:%20transparent%22%3E%3Chead%3E%3Ctitle%3E%E5%AE%89%E5%85%A8%E6%8B%A6%E6%88%AA%E4%BF%A1%E6%81%AF%3C%2Ftitle%3E%3Cstyle%3E.blockquote%2C%20body%2C%20button%2C%20code%2C%20dd%2C%20div%2C%20dl%2C%20dt%2C%20fieldset%2C%20form%2C%20h1%2C%20h2%2C%20h3%2C%20h4%2C%20h5%2C%20h6%2C%20input%2C%20legend%2C%20li%2C%20ol%2C%20p%2C%20pre%2C%20td%2C%20textarea%2C%20th%2C%20ul%7Bmargin:%200%3Bpadding:%200%3B%7Dbody%7Bfont-size:%2014px%3Bfont-family:%20%27Microsoft%20YaHei%27%3B%7D.sys-panel-cover%7Bposition:%20absolute%3Btop:%200%3Bleft:%200%3Bwidth:%20100%25%3Bheight:%20100%25%3Bbackground:%20%23000%3Bopacity:%200.6%3Bfilter:%20alpha%28opacity=60%29%3B%7D.sys-panel.in-center%7B%20position:%20absolute%3Btop:%2050%25%3Bleft:%2050%25%3Bmargin-left:%20-240px%3Bmargin-top:%20-240px%3B%7D.sys-panel%7Bborder-radius:%205px%3Bborder:%201px%20solid%20%23cdcdcd%3Bbackground-color:%20%23fff%3Bbox-shadow:%200%200%208px%20rgba%280%2C0%2C0%2C0.8%29%3Bwidth:%20550px%3Bbox-sizing:%20border-box%3Bpadding:%200%2030px%3Bpadding-bottom:20px%3B%7D.sys-panel%20.panel-title.danger%7Bcolor:%20%23a94442%3B%7D.sys-panel%20.panel-hd%7Bborder-bottom:%201px%20solid%20%23dcdcdc%3B%7D.sys-panel%20.panel-title%7Bfont-size:%2018px%3Bline-height:%202.5%3B%7D.sys-panel%20.panel-ft%7Bborder-top:%201px%20solid%20%23dcdcdc%3Bpadding:%2010px%200%3B%7D.sys-panel%20.alert%7Bpadding:%2010px%3Bborder-radius:%203px%3Bline-height:%201.8%3Bborder:%201px%20solid%20transparent%3Bheight:100px%3B%7D.sys-panel%20.alert-warn%7Bcolor:%20%238a6d3b%3Bbackground-color:%20%23fcf8e3%3Bborder-color:%20%23faebcc%3B%7D%3C%2Fstyle%3E%3C%2Fhead%3E%3Cbody%3E%3Cform%20id=%22form1%22%3E%3Cdiv%20class=%22sys-panel-cover%22%3E%3C%2Fdiv%3E%3Cdiv%20class=%22sys-panel%20in-center%22%20id=%22err-panel%22%3E%3Cdiv%20class=%22panel-hd%20mb20%22%3E%3Ch4%20class=%22panel-title%20danger%22%3E%E5%AE%89%E5%85%A8%E6%8F%90%E7%A4%BA%3C%2Fh4%3E%3C%2Fdiv%3E%3Cdiv%20class=%22panel-bd%20mb20%22%3E%3Cp%20class=%22alert%20alert-warn%22%3E%E5%AE%89%E5%85%A8%E6%8F%90%E7%A4%BA%EF%BC%9A%E6%82%A8%E7%9A%84%E8%AF%B7%E6%B1%82%E5%8F%AF%E8%83%BD%E5%AD%98%E5%9C%A8%E6%94%BB%E5%87%BB%E8%A1%8C%E4%B8%BA%2C%20%E5%B7%B2%E8%A2%ABEpointRASP%E6%8B%A6%E6%88%AA%3C%2Fbr%3E%E6%94%BB%E5%87%BB%E7%B1%BB%E5%9E%8B:%20%E3%80%90%25attack_name%25%E3%80%91%3C%2Fbr%3E%E6%94%BB%E5%87%BB%E7%BC%96%E5%8F%B7:%20%E3%80%90%25help_id%25%E3%80%91%3C%2Fbr%3E%E8%AF%B7%E6%B1%82ID:%20%25request_id%25%3C%2Fp%3E%3C%2Fdiv%3E%3C%2Fdiv%3E%3C%2Fform%3E%3C%2Fbody%3E%3C%2Fhtml%3E";
        String decode = URLDecoder.decode(html, "UTF-8");
        assert decode.contains(";");
    }
}
