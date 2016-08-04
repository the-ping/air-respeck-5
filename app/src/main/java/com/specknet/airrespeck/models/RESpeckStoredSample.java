package com.specknet.airrespeck.models;
/**
 * Created by esotsm54 on 7/25/2016.
 */
public class RESpeckStoredSample {
    private Long bs_timestamp;
    private Long rs_timestamp;
    private Integer seq;
    private Integer n;
    private Float mean_br;
    private Float sd_br;
    private Float act;

    public RESpeckStoredSample(Long bs_timestamp, Long rs_timestamp, Integer seq, Integer n, Float mean_br, Float sd_br, Float act) {
        super();
        this.bs_timestamp = bs_timestamp;
        this.rs_timestamp = rs_timestamp;
        this.seq = seq;
        this.n = n;
        this.mean_br = mean_br;
        this.sd_br = sd_br;
        this.act = act;
    }

    @Override
    public String toString() {
        return "RESpeckStoredSample (" + this.bs_timestamp + ", " +  this.rs_timestamp + ", " + this.seq + ", " + this.n + ", " + this.mean_br + ", " + this.sd_br + ", " + this.act + ")";
    }

    public Long getBs_timestamp() {return this.bs_timestamp;};
    public Long getRs_timestamp() { return this.rs_timestamp;};
    public Integer getSeq() { return this.seq;};
    public Integer getNBreaths() {return this.n;};
    public Float getMeanBr(){ return this.mean_br; };
    public Float getSdBr(){ return this.sd_br; };
    public Float getActivity(){ return this.act; };
}

