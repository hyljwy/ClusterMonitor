package com.intel.fangpei.SystemInfoCollector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.Swap;

import com.intel.fangpei.util.SystemUtil;
public class SysInfo {
	private static HashMap<String,String> HardWareRes =new HashMap<String,String>();
	private static SysInfo sysinfo = null;
	private SysInfo(){
	init();	
	}
	public static synchronized SysInfo GetSysHandler(){
		if(sysinfo == null){
			sysinfo = new SysInfo();
		}
			return sysinfo;
	}
	private void init(){
		SystemUtil.initSysParameter(HardWareRes);
	}
	public void Refresh() throws Exception{
		getCpuCount();
		getCpuTotal();
		testCpuPerc();
		getPhysicalMemory();
		getPlatformName();
		testGetOSInfo();
		testWho();
		testFileSystemInfo();
		HardWareRes.put("NetWork_FQDN",getFQDN());
		HardWareRes.put("NetWork_IP",getDefaultIpAddress());
		HardWareRes.put("NetWork_MAC",getMAC());
		testNetIfList();
	//	getEthernetInfo();
	}
 // 1.CPU��Դ��Ϣ
 // a)CPU��������λ������
 public static void getCpuCount() throws SigarException {
  Sigar sigar = new Sigar();
  try {
	  HardWareRes.put("CPU_Count", ""+sigar.getCpuInfoList().length);
  } finally {
   sigar.close();
  }
 }
 // b)CPU����������λ��HZ����CPU�������Ϣ
 public static void getCpuTotal() {
  Sigar sigar = new Sigar();
  CpuInfo[] infos;
  try {
   infos = sigar.getCpuInfoList();
   for (int i = 0; i < infos.length; i++) {// �����ǵ���CPU���Ƕ�CPU������
    CpuInfo info = infos[i];
    HardWareRes.put("CPU_Mhz",""+info.getMhz());// CPU������MHz
    HardWareRes.put("CPU_Vendor",info.getVendor());// ���CPU���������磺Intel
    HardWareRes.put("CPU_Celeron", info.getModel());// ���CPU������磺Celeron
    HardWareRes.put("CPU_Cache",""+info.getCacheSize());// ����洢������
   }
  } catch (SigarException e) {
   e.printStackTrace();
  }
 }
 // c)CPU���û�ʹ������ϵͳʹ��ʣ�������ܵ�ʣ�������ܵ�ʹ��ռ�����ȣ���λ��100%��
 public static void testCpuPerc() {
  Sigar sigar = new Sigar();
  // ��ʽһ����Ҫ�����һ��CPU�����
 /* CpuPerc cpu;
  try {
   cpu = sigar.getCpuPerc();
   printCpuPerc(cpu);
  } catch (SigarException e) {
   e.printStackTrace();
  }*/
  // ��ʽ���������ǵ���CPU���Ƕ�CPU������
  CpuPerc cpuList[] = null;
  try {
   cpuList = sigar.getCpuPercList();
  } catch (SigarException e) {
   e.printStackTrace();
   return;
  }
  String tmp = "";
  for (int i = 0; i < cpuList.length; i++) {
	  tmp+=    "|User |" + CpuPerc.format(cpuList[i].getUser())// �û�ʹ����
              +"|Sys  |" + CpuPerc.format(cpuList[i].getSys())// ϵͳʹ����
              +"|Wait |" + CpuPerc.format(cpuList[i].getWait())// ��ǰ�ȴ���
              +"|Nice |" + CpuPerc.format(cpuList[i].getNice())//
              +"|Idle |" + CpuPerc.format(cpuList[i].getIdle())// ��ǰ������
              +"|Total|" + CpuPerc.format(cpuList[i].getCombined());// �ܵ�ʹ����
  }
  HardWareRes.put("CPU_Perc", tmp);
 }
 // 2.�ڴ���Դ��Ϣ
 public static void getPhysicalMemory() {
  // a)�����ڴ���Ϣ
  Sigar sigar = new Sigar();
  Mem mem;
  try {
   mem = sigar.getMem();
   // �ڴ�����
   HardWareRes.put("Memory_Total","" + mem.getTotal() / 1024L + "K av");
   // ��ǰ�ڴ�ʹ����
   HardWareRes.put("Memory_Used", mem.getUsed() / 1024L + "K used");
   // ��ǰ�ڴ�ʣ����
   HardWareRes.put("Memory_Free", mem.getFree() / 1024L + "K free");
   // b)ϵͳҳ���ļ���������Ϣ
   Swap swap = sigar.getSwap();
   // ����������
   HardWareRes.put("Memory_Swap_Total", swap.getTotal() / 1024L + "K av");
   // ��ǰ������ʹ����
   HardWareRes.put("Memory_Swap_Used",  swap.getUsed() / 1024L + "K used");
   // ��ǰ������ʣ����
   HardWareRes.put("Memory_Swap_Free",  swap.getFree() / 1024L + "K free");
  } catch (SigarException e) {
   e.printStackTrace();
  }
 }
 // 3.����ϵͳ��Ϣ
 // a)ȡ����ǰ����ϵͳ�����ƣ�
 public static void getPlatformName() {
  String hostname = "";
  try {
   hostname = InetAddress.getLocalHost().getHostName();
  } catch (Exception exc) {
   Sigar sigar = new Sigar();
   try {
    hostname = sigar.getNetInfo().getHostName();
   } catch (SigarException e) {
    hostname = "localhost.unknown";
   } finally {
    sigar.close();
   }
  }
  HardWareRes.put("System_Name", hostname);
 }
 // b)ȡ��ǰ����ϵͳ����Ϣ
 public static void testGetOSInfo() {
  OperatingSystem OS = OperatingSystem.getInstance();
  // ����ϵͳ�ں������磺 386��486��586��x86
  HardWareRes.put("OS.getArch",OS.getArch());
		  HardWareRes.put("OS.getCpuEndian",OS.getCpuEndian());//
  HardWareRes.put("OS.getDataModel" , OS.getDataModel());//
  // ϵͳ����
  HardWareRes.put("OS.getDescription" ,OS.getDescription());
  HardWareRes.put("OS.getMachine" ,OS.getMachine());//
  // ����ϵͳ����
  HardWareRes.put("OS.getName" ,OS.getName());
  HardWareRes.put("OS.getPatchLevel" ,OS.getPatchLevel());//
  // ����ϵͳ������
  HardWareRes.put("System_Vendor", OS.getVendor());
  // ��������
  HardWareRes.put("OS.getVendorCodeName" , OS.getVendorCodeName());
  // ����ϵͳ����
  HardWareRes.put("OS.getVendorName", OS.getVendorName());
  // ����ϵͳ��������
  HardWareRes.put("OS.getVendorVersion" , OS.getVendorVersion());
  // ����ϵͳ�İ汾��
  HardWareRes.put("OS.getVersion" ,OS.getVersion());
 }
 // c)ȡ��ǰϵͳ���̱��е��û���Ϣ
 public static void testWho() {
  try {
   Sigar sigar = new Sigar();
   org.hyperic.sigar.Who[] who = sigar.getWhoList();
   StringBuilder sb = new StringBuilder();
   if (who != null && who.length > 0) {
    for (int i = 0; i < who.length; i++) {
    	org.hyperic.sigar.Who _who = who[i];
     sb.append(	 "|id    |"+i
    		 	+"|device|"+_who.getDevice()
    		    +"|host  |"+ _who.getHost()
    		    +"|time  |"+ _who.getTime()
    		    +"|user  |"+_who.getUser()// ��ǰϵͳ���̱��е��û���
    		    );
    }
   }
	HardWareRes.put("RegUser",sb.toString());
  } catch (SigarException e) {
   e.printStackTrace();
  }
 }
 // 4.��Դ��Ϣ����Ҫ��Ӳ�̣�
 // a)ȡӲ�����еķ���������ϸ��Ϣ��ͨ��sigar.getFileSystemList()�����FileSystem�б����Ȼ�������б�������
 public static void testFileSystemInfo() throws Exception {
	 StringBuilder sb = new StringBuilder();
  Sigar sigar = new Sigar();
  FileSystem fslist[] = sigar.getFileSystemList();
  //String dir = System.getProperty("user.home");// ��ǰ�û��ļ���·��
  for (int i = 0; i < fslist.length; i++) {
   FileSystem fs = fslist[i];
   /* �������̷�����
    �������ļ�������
    �ļ�ϵͳ���ͣ����� FAT32��NTFS
 �ļ�ϵͳ�����������籾��Ӳ�̡������������ļ�ϵͳ��*/
   sb.append("|DevName     |" + fs.getDevName()
		    +"|DirName     |" + fs.getDirName()
		    +"|Flags       |"+ fs.getFlags()
   			+"|SysTypeName |" + fs.getSysTypeName()
   			+"|TypeName    |" + fs.getTypeName()
   			+"|Type	       |" + fs.getType());
   FileSystemUsage usage = null;
   try {
    usage = sigar.getFileSystemUsage(fs.getDirName());
   } catch (SigarException e) {
    if (fs.getType() == 2)
     throw e;
    continue;
   }
   switch (fs.getType()) {
   case 0: // TYPE_UNKNOWN ��δ֪
    break;
   case 1: // TYPE_NONE
    break;
   case 2: // TYPE_LOCAL_DISK : ����Ӳ��
	    // �ļ�ϵͳ�ܴ�С
	    // �ļ�ϵͳʣ���С
	    // �ļ�ϵͳ���ô�С
	    // �ļ�ϵͳ�Ѿ�ʹ����
	    // �ļ�ϵͳ��Դ��������
	double usePercent = usage.getUsePercent() * 100D;
    sb.append("|Total  |" + usage.getTotal() + "KB"

     		 +"|Free   |" + usage.getFree() + "KB"

             +"|Avail  |" + usage.getAvail() + "KB"

             +"|Used   |" + usage.getUsed() + "KB"
    		 +"|Usage  |" + usePercent + "%");
    break;
   case 3:// TYPE_NETWORK ������
    break;
   case 4:// TYPE_RAM_DISK ������
    break;
   case 5:// TYPE_CDROM ������
    break;
   case 6:// TYPE_SWAP ��ҳ�潻��
    break;
   }
   sb.append("|DiskReads  |" + usage.getDiskReads()
		   	+"|DiskWrites |" + usage.getDiskWrites());
  }
  HardWareRes.put("Disk_info", sb.toString());
  return;
 }
 // 5.������Ϣ
 // a)��ǰ��������ʽ����
 public static String getFQDN() {
  Sigar sigar = null;
  try {
   return InetAddress.getLocalHost().getCanonicalHostName();
  } catch (UnknownHostException e) {
   try {
    sigar = new Sigar();
    return sigar.getFQDN();
   } catch (SigarException ex) {
    return null;
   } finally {
    sigar.close();
   }
  }
 }
 // b)ȡ����ǰ������IP��ַ
 public static String getDefaultIpAddress() {
  String address = null;
  try {
   address = InetAddress.getLocalHost().getHostAddress();
   // û�г����쳣��������ȡ����IPʱ�����ȡ���Ĳ�������ѭ�ص�ַʱ�ͷ���
   // ������ͨ��Sigar���߰��еķ�������ȡ
   if (!NetFlags.LOOPBACK_ADDRESS.equals(address)) {
    return address;
   }
  } catch (UnknownHostException e) {
   // hostname not in DNS or /etc/hosts
  }
  Sigar sigar = new Sigar();
  try {
   address = sigar.getNetInterfaceConfig().getAddress();
  } catch (SigarException e) {
   address = NetFlags.LOOPBACK_ADDRESS;
  } finally {
   sigar.close();
  }
  return address;
 }
 // c)ȡ����ǰ������MAC��ַ
 public static String getMAC() {
  Sigar sigar = null;
  try {
   sigar = new Sigar();
   String[] ifaces = sigar.getNetInterfaceList();
   String hwaddr = null;
   for (int i = 0; i < ifaces.length; i++) {
    NetInterfaceConfig cfg = sigar.getNetInterfaceConfig(ifaces[i]);
    if (NetFlags.LOOPBACK_ADDRESS.equals(cfg.getAddress())
      || (cfg.getFlags() & NetFlags.IFF_LOOPBACK) != 0
      || NetFlags.NULL_HWADDR.equals(cfg.getHwaddr())) {
     continue;
    }
    /*
     * ������ڶ������������������������Ĭ��ֻȡ��һ��������MAC��ַ�����Ҫ�������е���������������ĺ�����ģ�������޸ķ����ķ�������Ϊ�����Collection
     * ��ͨ����forѭ����ȡ���Ķ��MAC��ַ��
     */
    hwaddr = cfg.getHwaddr();
    break;
   }
   return hwaddr != null ? hwaddr : null;
  } catch (Exception e) {
   return null;
  } finally {
   if (sigar != null)
    sigar.close();
  }
 }
 // d)��ȡ������������Ϣ
 public static void testNetIfList() throws Exception {
  Sigar sigar = new Sigar();
  String ifNames[] = sigar.getNetInterfaceList();
  for (int i = 0; i < ifNames.length; i++) {
   String name = ifNames[i];
   NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
  //HardWareRes.put("NetWork_Name",name);// �����豸��
  //HardWareRes.put("NetWork_IP",ifconfig.getAddress());// IP��ַ
  //HardWareRes.put("NetWork_NetMask",ifconfig.getNetmask());// ��������
   if ((ifconfig.getFlags() & 1L) <= 0L) {
    print("!IFF_UP...skipping getNetInterfaceStat");
    continue;
   }
   try {
    NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
    HardWareRes.put("NetWork_RxPackets",""+ifstat.getRxPackets());// ���յ��ܰ�����
    HardWareRes.put("NetWork_TxPackets",""+ifstat.getTxPackets());// ���͵��ܰ�����
    HardWareRes.put("NetWork_RxBytes",""+ifstat.getRxBytes());// ���յ������ֽ���
    HardWareRes.put("NetWork_TxBytes",""+ifstat.getTxBytes());// ���͵����ֽ���
    HardWareRes.put("NetWork_RxErrors",""+ifstat.getRxErrors());// ���յ��Ĵ������
    HardWareRes.put("NetWork_TxErrors",""+ifstat.getTxErrors());// �������ݰ�ʱ�Ĵ�����
    HardWareRes.put("NetWork_RxDropped",""+ifstat.getRxDropped());// ����ʱ�����İ���
    HardWareRes.put("NetWork_TxDropped",""+ifstat.getTxDropped());// ����ʱ�����İ���
   } catch (SigarNotImplementedException e) {
   } catch (SigarException e) {
    print(e.getMessage());
   }
  }
 }
 static void print(String msg) {
  System.out.println(msg);
 }
 // e)һЩ��������Ϣ
 public static void getEthernetInfo() {
  Sigar sigar = null;
  try {
   sigar = new Sigar();
   String[] ifaces = sigar.getNetInterfaceList();
   for (int i = 0; i < ifaces.length; i++) {
    NetInterfaceConfig cfg = sigar.getNetInterfaceConfig(ifaces[i]);
    if (NetFlags.LOOPBACK_ADDRESS.equals(cfg.getAddress())
      || (cfg.getFlags() & NetFlags.IFF_LOOPBACK) != 0
      || NetFlags.NULL_HWADDR.equals(cfg.getHwaddr())) {
     continue;
    }
    System.out.println("cfg.getAddress() = " + cfg.getAddress());// IP��ַ
    System.out
      .println("cfg.getBroadcast() = " + cfg.getBroadcast());// ���ع㲥��ַ
    System.out.println("cfg.getHwaddr() = " + cfg.getHwaddr());// ����MAC��ַ
    System.out.println("cfg.getNetmask() = " + cfg.getNetmask());// ��������
    System.out.println("cfg.getDescription() = "
      + cfg.getDescription());// ����������Ϣ
    System.out.println("cfg.getType() = " + cfg.getType());//
    System.out.println("cfg.getDestination() = "
      + cfg.getDestination());
    System.out.println("cfg.getFlags() = " + cfg.getFlags());//
    System.out.println("cfg.getMetric() = " + cfg.getMetric());
    System.out.println("cfg.getMtu() = " + cfg.getMtu());
    System.out.println("cfg.getName() = " + cfg.getName());
    System.out.println();
   }
  } catch (Exception e) {
   System.out.println("Error while creating GUID" + e);
  } finally {
   if (sigar != null)
    sigar.close();
  }
 }
	public static byte[] serialize(HashMap<String, String> hashMap){ 
        try { 
        ByteArrayOutputStream mem_out = new ByteArrayOutputStream(); 
            ObjectOutputStream out = new ObjectOutputStream(mem_out); 
 
            out.writeObject(hashMap); 
 
            out.close(); 
           mem_out.close(); 
 
           byte[] bytes =  mem_out.toByteArray(); 
           return bytes; 
        } catch (IOException e) { 
            return null; 
        } 
    }
    public static HashMap<String, String> deserialize(byte[] bytes){ 
        try { 
            ByteArrayInputStream mem_in = new ByteArrayInputStream(bytes); 
            ObjectInputStream in = new ObjectInputStream(mem_in); 
 
            HashMap<String, String> hashMap = (HashMap<String, String>)in.readObject(); 
 
             in.close(); 
             mem_in.close(); 
 
             return hashMap; 
        } catch (StreamCorruptedException e) { 
            return null; 
        } catch (ClassNotFoundException e) { 
            return null; 
        }   catch (IOException e) { 
            return null; 
        } 
     }
 public byte[]  GetSysInfoBytes(){
	return serialize(HardWareRes);
 }
 public HashMap<String,String> GetSysInfoMap(byte[] b){
	 return deserialize(b);
 }
 public HashMap<String,String> GetSysInfoMap(){
	 return HardWareRes;
 }
}

