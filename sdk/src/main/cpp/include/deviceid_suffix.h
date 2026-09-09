#ifndef DEVICEID_SUFFIX_PROTECT
#define DEVICEID_SUFFIX_PROTECT

/*�豸ID�ŷ������
�豸ID��Ϊ32λ����
��14λ��ʾ����ţ�ԭ����ÿ���豸��һ�������
��18λ��ʾ�豸�ţ�������2^10Ϊ��С���䵥λ��Ԥ��2^18�ռ䡣
ID��0��0xffffffff����
�����������0xff000000/8
ý���������0xfe000000/8
����������  0xfd000000/8

����					ID��Χ	16����  			 ID��Χ					     ʹ��״̬      - .


PC����
PC�˹������			0x00000001 -- <40000				 1 �C  <262144			  ��ʹ��
PC��Զ�̿������	    0x00040000 -- <80000			262144 �C  <524288				δ��
PC���û��������		0x00080000 -- <c0000			524288 �C  <786432
PC���������			0x000c0000 -- <100000			786432 �C  <1048576

�ֻ�Android
�ֻ��˹������			0x00100000 -- <140000			1048576-  <1310720
�ֻ���Զ�̿������		0x00140000 -- <180000			1310720-  <1572864
�ֻ����û��������		0x00180000 -- <1c0000			1572864-  <1835008		ʹ��
�ֻ����������			0x001c0000 -- <200000			1835008-  <2097152

Ѱ����Android
Ѱ�������				0x00200000 -- <240000			2097152-  <2359296		ʹ��
Ѱ�������2				0x00240000 -- <280000			2359296-  <2621440
Ѱ�������3				0x00280000 -- <2c0000			2621440-  <2883584
Ѱ�������4				0x002c0000 -- <300000			2883584-  <3145728

�㲥Android
�㲥�ն�������		0x00300000 -- <340000			3145728-  <3407872		ʹ��
�㲥�ն�������2		0x00340000 -- <380000			3407872-  <3670016
�㲥�ն�������3		0x00380000 -- <3c0000			3670016-  <3932160
�㲥�ն�������4		0x003c0000 -- <400000			3932160-  <4194304


Arm�ն�
�����ն�				0x00400000 -- <440000			4194304-  <4456448      ʹ��
�澯�ն�				0x00440000 -- <480000			4456448-  <4718592
��Դ�����豸(�ն�)		0x00480000 -- <4c0000			4718592-  <4980736		ʹ��
��·�����豸(�ն�)		0x004c0000 -- <500000			4980736-  <5242880



�ֻ�IOS
IOS�˹������
IOS��Զ�̿������
IOS���û��������
IOS���������

����������� 0xfcfe0000
����������� 0xfcfd0000
*/

//����������� 0xfcfe0000
//����������� 0xfcfd0000

//����ʹ�����豸ID����ͬһ��,����ע�᷽ʽ��һ����
#define DEVICE_CFG_REGCTRL		0xfcff0000 //���еĹ�˾ע����������ID��������
#define DEVICE_CFG_PROVIDER		0xfcfe0000 //���еĹ��������ID��������
#define DEVICE_CFG_INTERNAL		0xfcfd0000 //���еĹ������������ID��������
#define DEVICE_CFG_MONITOR		0xfcfc0000 //���е��ػ������ID��������

#define SERVER_DEVICE_ID_MASK		0xff000000
#define CLIENT_DEVICE_ID_MASK		0xffff0000

#define SERVER_DEVICE_MASK		8 //Ĭ�ϵķ�������mask
#define CLIENT_DEVICE_MASK		16//Ĭ�ϵ��ն���mask


#define DEVICE_PC_SYSTEM			0x00000000
#define DEVICE_PC_CONTROL			0x00040000
#define DEVICE_PC_OPERATOR			0x00080000

#define DEVICE_ANDROID_SYSTEM		0x00100000
#define DEVICE_ANDROID_CONTROL		0x00140000
#define DEVICE_ANDROID_OPERATOR		0x00180000

#define DEVICE_PAGING_SYSTEM		0x00200000
#define DEVICE_PAGING_CONTROL		0x00240000
#define DEVICE_PAGING_OPERATOR		0x00280000

#define DEVICE_ORDER_SYSTEM			0x00300000
#define DEVICE_ORDER_CONTROL		0x00340000
#define DEVICE_ORDER_OPERATOR		0x00380000
#define DEVICE_ORDER_CAPTURE        0x003c0000

#define DEVICE_ARM_PLAY				0x00400000
#define DEVICE_ARM_WARNING			0x00440000
#define DEVICE_ARM_AUDIOINPUT		0x00480000
#define DEVICE_ARM_SHORTCONTROL		0x004c0000

//����ϵͳר���豸��
#define DEVICE_MEET_MASKBIT		14
#define DEVICE_MEET_ID_MASK		0xfffc0000

#define DEVICE_MEET_DB				0x01000000  //�������ݿ�(�����̨)
#define DEVICE_MEET_SERVICE			0x01040000  //�����ˮ����
#define DEVICE_MEET_PROJECTIVE		0x01080000  //����ͶӰ��  
#define DEVICE_MEET_CAPTURE			0x010c0000  //�������ɼ��豸
#define DEVICE_MEET_CLIENT			0x01100000  //����ͻ��� 
#define DEVICE_MEET_PUBLISH			0x01140000  //���鷢��
#define DEVICE_MEET_ONEKEYSHARE		0x01200000  //����һ��ͬ�� 

//����
//#define DEVICE_MEET_OPTION1			0x01140000
#define DEVICE_MEET_OPTION2			0x01180000
#define DEVICE_MEET_OPTION3			0x011c0000


#define CHECKE_DEVICEID(x, devtype, devmask) \
	devtype = (x & g_idmask[devmask]);

#endif
