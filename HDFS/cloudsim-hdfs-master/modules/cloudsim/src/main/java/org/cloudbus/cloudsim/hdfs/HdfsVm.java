package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class HdfsVm extends Vm {

    protected int hdfsType;

    /**
     * یک شی Vm جدید ایجاد می کند.
     *
     * @param id                وid منحصر به فرد VM
     * @param userId            وid مالک VM
     * @param mips              the mips
     * @param numberOfPes       مقدار CPU
     * @param ram               مقدار ram
     * @param bw               مقدار پهنای باند
     * @param size              اندازه اندازه تصویر VM (مقدار فضای ذخیره سازی که حداقل در ابتدا استفاده می کند).
     * @param vmm               مانیتور ماشین مجازی
     * @param cloudletScheduler خط‌مشی cloudletScheduler برای زمان‌بندی cloudlets
     * @pre id >= 0
     * @pre userId >= 0
     * @pre size > 0
     * @pre ram > 0
     * @pre bw > 0
     * @pre cpus > 0
     * @pre priority >= 0
     * @pre cloudletScheduler != null
     * @post $none
     */
    public HdfsVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
    }

    public int getHdfsType() {
        return hdfsType;
    }

    public void setHdfsType(int hdfsType) {
        this.hdfsType = hdfsType;
    }
}
