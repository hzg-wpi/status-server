package wpn.hdri.ss.data2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */

public class SingleRecord {
    public final int id; //attr.id
    public final long r_t; //rea_timestamp
    public final long w_t; //write_timestamp
    public final long value; //value bits
    //TODO do we need padding here?

    public SingleRecord(int id, long r_t, long w_t, long value) {
        this.id = id;
        this.r_t = r_t;
        this.w_t = w_t;
        this.value = value;
    }
}