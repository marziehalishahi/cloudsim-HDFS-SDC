package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//  یک broker معمولی است، اما هیچ ماشین مجازی را در هنگام اجرا اختصاص نمی‌دهد،
// تنها نقش آن ارسال cloudlets تکراری به vms مناسب است.
public class HdfsReplicationBroker extends HdfsDatacenterBroker {

    // used only to print prettier logsبرای چاپ زیباتر از داینامیک استفاده کردیم
    DecimalFormat df = new DecimalFormat("#.###");

    // سازندگان

    public HdfsReplicationBroker(String name) throws Exception {
        super(name);
    }

    public HdfsReplicationBroker(String name, int cloudletStartId) throws Exception {
        super(name, cloudletStartId);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // درخواست ویژگی های منبع
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // پاسخ ویژگی های منابع
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // پاسخ ایجاد VM
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            // وcloudlet تمام شده برگشت
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            //اگر شبیه سازی به پایان برسد
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;

            /**
             *  HDFS tags
             */

            // اcloudlet تمام شده برگشت
            case CloudSimTags.HDFS_CLIENT_CLOUDLET_RETURN:
                processClientCloudletReturn(ev);
                break;
            //یک cloudlet DN تمام شده برمی گردد، این برای RePLICATION استفاده می شود
            case CloudSimTags.HDFS_DN_CLOUDLET_RETURN:
                processSendReplicaCloudlet(ev);
                break;
            // نNAMENODE لیستی از vms را برمی‌گرداند که در آن بلوک بنویسد
            case CloudSimTags.HDFS_NAMENODE_RETURN_DN_LIST:
                processSendDataCloudlet(ev);
                break;
            // Replication Broker
            // باید VM ها و map Vms به Datanodes را بشناسد، این اطلاعات توسط broker اصلی برای او ارسال می شود.
            case CloudSimTags.HDFS_REP_BROKER_ADD_DN:
                processDataNodeInformation(ev);
                break;

            // سایر تگ های ناشناخته با این روش پردازش می شوند
            default:
                processOtherEvent(ev);
                break;
        }
    }

    protected void processDataNodeInformation(SimEvent ev){
        int[] data = (int[]) ev.getData();
        int currentDataNodeId = data[0];
        int currentDatacenterId = data[1];
        int currentRackid = data[2];    // unused
        int currentStorageCapacity = data[3];   // unused

        getVmsToDatacentersMap().put(currentDataNodeId, currentDatacenterId);
        getVmsCreatedList().add(VmList.getById(getVmList(), currentDataNodeId));
    }

    @Override
    protected void submitDNCloudlets() {
        int vmIndex = 0;
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm vm;
            // اگر کاربر این cloudlet را باند نکرده باشد و هنوز اجرا نشده باشد
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // به vm خاص ارسال کنید
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { //  وvm ایجاد نشد
                    if(!Log.isDisabled()) {
                        Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Postponing execution of Data Cloudlet ",
                                cloudlet.getCloudletId(), ": bound VM not available");
                    }
                    continue;
                }
            }

            if (!Log.isDisabled()) {
                Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Sending Data Cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            // این روش باید به طور خودکار Datacenter را که ماشین مجازی DN در آن قرار دارد بدون مشکل پیدا کند
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT, cloudlet);

            cloudletsSubmitted++;
            currentCloudletMaxId = Math.max(cloudlet.getCloudletId(), currentCloudletMaxId);
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);
        }

        // وcloudlets را از لیست انتظار حذف کنید
        getCloudletList().removeAll(successfullySubmitted);
    }

    @Override
    //شبیه به processSendDataCloudlet است اما کل HdfsCloudlet را به عنوان یک رویداد برای ارسال به مقصد بعدی دریافت می کند.
    protected void processSendReplicaCloudlet(SimEvent ev) {

        HdfsCloudlet originalCloudlet = (HdfsCloudlet) ev.getData();

        Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": ReplicationCloudlet ", originalCloudlet.getCloudletId(),
                ": the block has been read, sending it to the Data Node...");

        stagedCloudlet = HdfsCloudlet.cloneCloudletAssignNewId(originalCloudlet, currentCloudletMaxId + 1);

        // شid اصلی vm را ذخیره کنید تا بتوانیم بلاک آن را در DN پیگیری کنیم
        stagedCloudlet.setSourceVmId(originalCloudlet.getVmId());

        // لیست vms مقصد را دریافت کنید
        List<Integer> destinationVms = originalCloudlet.getDestVmIds();

        if (destinationVms.isEmpty()){
            Log.printLine(df.format(CloudSim.clock()) + ": " + getName() + ": The replication pipeline is over");
            return;
        }
        // DN VM
        // را به عنوان id VM جدید برای کلودلت تنظیم کنید

        stagedCloudlet.setVmId(destinationVms.get(0));

        // اولین vm را از لیست vms مقصد حذف می کنم و به destVmIds اضافه می کنم
        destinationVms.remove(0);
        stagedCloudlet.setDestVmIds(destinationVms);

        // یا می توانید از متد bind استفاده کنید که همین کار را انجام می دهد
        // bindCloudletToVm(cloudlet.getCloudletId(), cloudlet.getVmId());

        // وcloudlet را به لیست ابرهای ارسالی اضافه کنید
        getCloudletList().add(stagedCloudlet);

        /* ri-eseguiamo questo metodo, che ora troverà il nuovo unbound cloudlet nella lista, e lo invierà
        alla VM appropriata, inoltre settando la posizione del broker uguale a quella del client nella topology,
        avremo una corretta simulazione del delay per l'invio del file tramite network
        */
        /* این متد را مجددا اجرا کنید که اکنون اcloudlet جدید را در لیست پیدا کرده و به VM مناسب ارسال کنید،
        شبیه سازی تاخیر برای ارسال فایل ها از طریق شبکه
         */
        submitDNCloudlets();

    }

    @Override
    protected void processResourceCharacteristics(SimEvent ev) {

    }

    @Override
    public void submitVmList(List<? extends Vm> list) {
        getVmList().addAll(list);
        setVmsCreatedList(list);
    }

    @Override
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": VM #", vmId,
                    " has been created in Datacenter#", datacenterId-1, ", Host #",
                    VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
        } else {
            Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Creation of VM #", vmId,
                    " failed in Datacenter#", datacenterId-1);
        }

        incrementVmsAcks();

        // تمام VM های درخواستی ایجاد شده اند
        if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {

            submitCloudlets();
        } else {
            //همه Acks دریافت شدند، اما برخی از ماشین های مجازی ایجاد نشدند
            if (getVmsRequested() == getVmsAcks()) {
                // شid دیتاسنتر بعدی را که استفاده نشده است پیدا کنید
                for (int nextDatacenterId : getDatacenterIdsList()) {
                    if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
                        createVmsInDatacenter(nextDatacenterId);
                        return;
                    }
                }

                // همه دیتاسنترها قبلاً query شده اند
                if (getVmsCreatedList().size() > 0) { // if some vm were created
                    submitCloudlets();
                } else { // vms ایجاد نشده است. abort
                    Log.printLine(df.format(CloudSim.clock()) + ": " + getName()
                            + ": none of the required VMs could be created. Aborting");
                    finishExecution();
                }
            }
        }
    }
}
