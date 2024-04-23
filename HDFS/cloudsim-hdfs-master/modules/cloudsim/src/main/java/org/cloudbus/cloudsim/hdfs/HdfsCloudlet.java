package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import java.util.List;

import static org.cloudbus.cloudsim.core.CloudSimTags.HDFS_DN;

public class HdfsCloudlet extends Cloudlet {

    // نوع HDFS: یا Client یا Data Node، به طور پیش فرض Data Node خواهد بود
    protected int hdfsType;

    // این متغیر, شناسه vm اصلی مرتبط با این cloudlet را در خود جای داده است
    protected int sourceVmId;

    //این لیست شامل vmهای مقصد است که datablock در آنجا نوشته می شود.
    protected List<Integer> destVmIds;

    // فایل مورد نیاز (Cloudlet فهرستی از فایل های مورد نیاز دارد، اما برای این مدل فقط به یک فایل نیاز داریم)
    protected File requiredFile;

    //این متغیر اندازه datablock ای که باید نوشته شود را نگه می دارد.
    protected int blockSize;

    // این متغیر تعداد کپی های مورد نظر برای فایل مرتبط با این cloudlet را نگه می دارد
    protected int replicaNum;

    /*
     * در صورت نیاز (من hdfsBlock را اضافه کردم که حاوی اطلاعات نوشتن فایل در DN است)
     */

    public HdfsCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                        UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                        List<String> fileList, int blockSize, int replicaNum) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, fileList);
        this.blockSize = blockSize;
        this.replicaNum = replicaNum;
        // به طور پیش فرض نوع آن Data Node خواهد بود
        // این به این دلیل است که من این تغییر را پس از نوشتن تمام کدهای انتقال فایل انجام دادم
        this.hdfsType = HDFS_DN;
    }

    // سازنده اگر تعداد کپی ها مشخص نشده باشد (مقدار روی 0 تنظیم شده است)
    public HdfsCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                        UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                        List<String> fileList, int blockSize) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, fileList);
        this.blockSize = blockSize;
        this.replicaNum = 0;
        // به طور پیش فرض نوع آن Data Node خواهد بود
        // این به این دلیل است که من این تغییر را پس از نوشتن تمام کدهای انتقال فایل انجام دادم
        this.hdfsType = HDFS_DN;
    }

    //به طور کلی، این روش یک کلون از شی اصلی HdfsCloudlet با یک شناسه جدید ایجاد می کند
    // و ویژگی های دیگری مانند utilization models، شناسه کاربر و شناسه های VM مقصد را تنظیم می کند
    public static HdfsCloudlet cloneCloudletAssignNewId(HdfsCloudlet cl, int newId){

        long cloudletLength = cl.getCloudletLength();
        int pesNumber = cl.getNumberOfPes();
        long cloudletFileSize = cl.getCloudletFileSize();
        long cloudletOutputSize = cl.getCloudletOutputSize();
        // TODO: در حال حاضر من همه چیز را به عنوان Utilization Model full مجدداً نمونه‌سازی می‌کنم، باید بررسی کنم که کلودلت اصلی از کدام مدل استفاده استفاده می‌کند،
        //  اما فعلاً فقط از full استفاده می‌کنم
        UtilizationModel utilizationModelCpu = new UtilizationModelFull();
        UtilizationModel utilizationModelRam = new UtilizationModelFull();
        UtilizationModel utilizationModelBw = new UtilizationModelFull();
        List<String> fileList = cl.getRequiredFiles();
        int blockSize = cl.getBlockSize();

        HdfsCloudlet newCl = new HdfsCloudlet(newId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
                utilizationModelCpu, utilizationModelRam, utilizationModelBw, fileList, blockSize);

        // وid کاربر را تنظیم کنید، زیرا بخشی از سازنده نیست
        int userId = cl.getUserId();
        newCl.setUserId(userId);

        // وid vms مقصد را تنظیم کنید
        newCl.setDestVmIds(cl.getDestVmIds());

        return newCl;
    }

    // Getters and Setters

    public int getHdfsType() {
        return hdfsType;
    }

    public void setHdfsType(int hdfsType) {
        this.hdfsType = hdfsType;
    }
//

    public int getSourceVmId() {
        return sourceVmId;
    }
//    این متد یک دریافت کننده برای ویژگی sourceVmId است.
    //    مقدار فعلی صفت sourceVmId را برمی گرداند
    public void setSourceVmId(int sourceVmId) {
        this.sourceVmId = sourceVmId;
    }
//    از این متد ها برای دریافت و تنظیم ویژگی sourceVmId استفاده می شود
//    که نشان دهنده شناسه vm اصلی مرتبط با cloudlet است.

    public List<Integer> getDestVmIds() {
        return destVmIds;
    }

    public void setDestVmIds(final List<Integer> destVmIds) {
        this.destVmIds = destVmIds;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getReplicaNum() {
        return replicaNum;
    }

    public void setReplicaNum(int replicaNum) {
        this.replicaNum = replicaNum;
    }
}
