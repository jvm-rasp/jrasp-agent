import com.alibaba.druid.wall.WallUtils;
import org.junit.Test;

public class SQLTest {

    @Test
    public void sqlTest() {
        String sql="create procedure `epoint_proc_alter`() begin if not exists (select null from information_schema.columns where table_schema = database() and table_name = 'frame_ip_lockinfo' and column_name = 'CreateDate') then alter table frame_ip_lockinfo add column CreateDate datetime; end if; end;";
        boolean result = WallUtils.isValidateMySql(sql);
        System.out.println(result);
    }
}
