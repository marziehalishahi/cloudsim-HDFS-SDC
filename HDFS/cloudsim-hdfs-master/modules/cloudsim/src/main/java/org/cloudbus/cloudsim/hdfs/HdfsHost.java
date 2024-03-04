package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.HarddriveStorage;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.List;

// مشکل در حال حاضر استفاده از این حافظه هارد درایو در شبیه سازی است!!

// وhost Hdfs از کلاس ذخیره سازی هارد دیسک که قبلاً در Cloud Sim وجود دارد برای شبیه سازی فضای ذخیره سازی استفاده می کند.
// یک host معمولی از یک "long" ساده برای پیگیری فضای ذخیره سازی استفاده می کند.

public class HdfsHost extends Host {

    private int rackId;

    // TODO: این در حال حاضر کاملاً بی فایده است دلیل: به یک هاست در حال حاضر یک یا چند هارد دیسک اختصاص داده نشده است.
    //به Datacenter تخصیص داده می شوند و هر میزبان فقط دارای یک عدد است که نشان دهنده ظرفیت ذخیره سازی است.
    // من این قسمت را فعلاً در اینجا می گذارم تا در آینده مورد نیاز باشد.
    private HarddriveStorage properStorage;

    /**
     * فعلا از این سازنده استفاده نکنید!!
     */
    public HdfsHost(int id, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
                    HarddriveStorage hddStorage, List<? extends Pe> peList, VmScheduler vmScheduler) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
        properStorage = hddStorage;
    }

    public HdfsHost(int id, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
                    List<? extends Pe> peList, VmScheduler vmScheduler) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
    }

    public HarddriveStorage getProperStorage() {
        return properStorage;
    }

    public void setProperStorage(HarddriveStorage properStorage) {
        this.properStorage = properStorage;
    }

    public int getRackId() {
        return rackId;
    }

    public void setRackId(int rackId) {
        this.rackId = rackId;
    }
}
