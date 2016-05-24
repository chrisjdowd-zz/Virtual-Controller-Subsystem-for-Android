#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <time.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <sys/limits.h>
#include <sys/poll.h>

#include <linux/fb.h>
#include <linux/kd.h>
#include <linux/input.h>
#include <linux/uinput.h>

#include <android/log.h>
#define TAG "EventEmulator::JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#include "EventEmulator.h"



/* Debug tools
 */
 int g_debug = 0;


void debug(char *szFormat, ...)
{
	if (g_debug == 0) return;
	//if (strlen(szDbgfile) == 0) return;

	char szBuffer[4096]; //in this buffer we form the message
	const size_t NUMCHARS = sizeof(szBuffer) / sizeof(szBuffer[0]);
	const int LASTCHAR = NUMCHARS - 1;
	//format the input string
	va_list pArgs;
	va_start(pArgs, szFormat);
	// use a bounded buffer size to prevent buffer overruns.  Limit count to
	// character size minus one to allow for a NULL terminating character.
	vsnprintf(szBuffer, NUMCHARS - 1, szFormat, pArgs);
	va_end(pArgs);
	//ensure that the formatted string is NULL-terminated
	szBuffer[LASTCHAR] = '\0';

	LOGD(szBuffer);
	//TextCallback(szBuffer);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	debug("eventinterceptor native lib loaded.");
	return JNI_VERSION_1_2; //1_2 1_4
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
	debug("eventinterceptor native lib unloaded.");
}

static struct typedev {
	struct pollfd ufds;
	char *device_path;
	char *device_name;
} *pDevs = NULL;
struct pollfd *ufds;
static int nDevsCount;

const char *device_path = "/dev/input";

int ePolling = 0;
struct input_event event;
int c;
int i;
int pollres;
int get_time = 0;
char *newline = "\n";
uint16_t get_switch = 0;
struct input_event event;
int version;
int exclusive = 0;

int dont_block = -1;
int event_count = 0;
int sync_rate = 0;
int64_t last_sync_time = 0;
const char *device = NULL;

int vMapper = 0;


static int open_device(int index)
{
	if (index >= nDevsCount || pDevs == NULL) return -1;
	debug("open_device prep to open");
	char *device = pDevs[index].device_path;

	debug("open_device call %s", device);
    int version;
    int fd;

    char name[80];
    char location[80];
    char idstr[80];

    fd = open(device, O_RDWR);
    if(fd < 0) {
		pDevs[index].ufds.fd = -1;

		pDevs[index].device_name = NULL;
		debug("could not open %s, %s", device, strerror(errno));
        return -1;
    }
    debug("fd for device %s is %d", device, fd);
	pDevs[index].ufds.fd = fd;
	ufds[index].fd = fd;

    name[sizeof(name) - 1] = '\0';
    if(ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
        debug("could not get device name for %s, %s", device, strerror(errno));
        name[0] = '\0';
    }

	debug("Device %d: %s: %s", index, device, name);
	pDevs[index].device_name = strdup(name);
    return 0;
}

int remove_device(int index)
{
	debug("remove device");
	if (index >= nDevsCount || pDevs == NULL ) return -1;
	int count = nDevsCount - index - 1;
	debug("count: %d",count);
	debug("freeing %s",pDevs[index].device_path);
	debug("index %d",index);
	free(pDevs[index].device_path);
	debug("freeing: %s",pDevs[index].device_name);
	free(pDevs[index].device_name);
	debug("calling memmove");
	memmove(&pDevs[index], &pDevs[index+1], sizeof(pDevs[0]) * count);
	debug("decrimenting devcount");
	nDevsCount--;
	return 0;
}

int createDevice(int xres, int yres){
	int fd, ret ,i;
	struct uinput_dev tsdev, condev;

	debug("Creating touchscreen");
	fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
	if(fd<0){
		debug("Could not open uinput");
		return -1;
	}

	memset(&tsdev, 0, sizeof(tsdev));
	tsdev.absmin[ABS_X]=0;
	tsdev.absmax[ABS_X]=xres;
	tsdev.absfuzz[ABS_X]=0;
	tsdev.absflat[ABS_X ]=0;
	tsdev.absmin[ABS_Y]=0;
	tsdev.absmax[ABS_Y]=yres;
	tsdev.absfuzz[ABS_Y]=0;
	tsdev.absflat[ABS_Y ]=0;

	snprintf(tsdev.name, UINPUT_MAX_NAME_SIZE, "MAPPER TS");
	tsdev.id.bustype = 0x18;
	tsdev.id.vendor = 0x0000;
	tsdev.id.product = 0x0000;
	tsdev.id.version = 0;

	ioctl(fd, UI_SET_EVBIT, EV_KEY);
	ioctl(fd, UI_SET_KEYBIT, BTN_TOUCH);
	ioctl(fd, UI_SET_EVBIT, EV_ABS);
	ioctl(fd, UI_SET_ABSBIT, ABS_X);
	ioctl(fd, UI_SET_ABSBIT, ABS_Y);
	ioctl(fd, UI_SET_EVBIT, EV_SYN);
	ioctl(fd, UI_SET_PROPBIT, INPUT_PROP_DIRECT);


	ret = write(fd, &tsdev, sizeof(tsdev));
	if(ret<0){
		debug("Could not write to uinput");
		return -1;
	}
	ret = ioctl(fd, UI_DEV_CREATE);
	if(ret<0){
		debug("Could not create touchscreen");
		return -1;
	}


	int fs = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
	if(fs<0){
		debug("Could not open uinput");
		return -1;
	}
	debug("Creating gamepad");
	int keys[] = {
		0x130, //a
		0x131, //b
		0x132, //c
		0x133, //x
		0x134, //y
		0x135, //z
		0x136, //lb
		0x137, //rb
		0x138, //l2
		0x139, //r2
		0x13a, //select
		0x13b, //start
		0x13c, //mode
		0x13d, //l3
		0x13e, //r3
		103, //up
		108, //down
		105, //left
		106, //right
		0x110, //mouse left click
		102, //home gpio "move home"
		158, //back gpio
		116, //power gpio
		217, //search gpio
		113, //vol mute
		114, //vol down gpio
		115, //vol up gpio
		139, //menu
		172, //home
	};
	int keylen = sizeof(keys)/sizeof(keys[0]);
	int axis[] = {
			0x00,
			0x01,
			0x02,
			0x03,
			0x04,
			0x05,
			0x30,
			0x31
	};
	int axislen = sizeof(axis)/sizeof(axis[0]);

	ioctl(fs, UI_SET_EVBIT, EV_KEY);
	for(i=0; i<keylen; i++){
			ioctl(fs, UI_SET_KEYBIT, keys[i]);
		}
	ioctl(fs, UI_SET_EVBIT, EV_ABS);
	for(i=0; i<axislen; i++){
		ioctl(fs, UI_SET_ABSBIT, axis[i]);
	}
	ioctl(fs, UI_SET_EVBIT, EV_REL);
	ioctl(fs, UI_SET_RELBIT, REL_X);
	ioctl(fs, UI_SET_RELBIT, REL_Y);
	ioctl(fs, UI_SET_EVBIT, EV_SYN);

	memset(&condev, 0, sizeof(condev));

	condev.absmin[ABS_X]=0;
	condev.absmax[ABS_X]=65535;
	condev.absfuzz[ABS_X]=0;
	condev.absflat[ABS_X ]=4096;
	condev.absmin[ABS_Y]=0;
	condev.absmax[ABS_Y]=65535;
	condev.absfuzz[ABS_Y]=0;
	condev.absflat[ABS_Y ]=4096;

	condev.absmin[ABS_RX]=0;
	condev.absmax[ABS_RX]=65535;
	condev.absfuzz[ABS_RX]=0;
	condev.absflat[ABS_RX]=4096;
	condev.absmin[ABS_RY]=-0;
	condev.absmax[ABS_RY]=65535;
	condev.absfuzz[ABS_RY]=0;
	condev.absflat[ABS_RY ]=4096;

	condev.absmin[ABS_Z]=0;
	condev.absmax[ABS_Z]=255;
	condev.absfuzz[ABS_Z]=0;
	condev.absflat[ABS_Z ]=0;
	condev.absmin[ABS_RZ]=0;
	condev.absmax[ABS_RZ]=255;
	condev.absfuzz[ABS_RZ]=0;
	condev.absflat[ABS_RZ ]=0;

	snprintf(condev.name, UINPUT_MAX_NAME_SIZE, "MAPPER GAMEPAD");
	condev.id.bustype = BUS_USB;
	condev.id.vendor = 0x0001;
	condev.id.product = 0x0002;
	condev.id.version = 1;

	int res = write(fs, &condev, sizeof(condev));
	if(res<0){
		debug("Could not write to uinput");
		return -1;
	}
	res = ioctl(fs, UI_DEV_CREATE);
	if(res<0){
		debug("Could not create gamepad: %s", strerror(errno));
		return -1;
	}

	return ret&res;
}

int getExclusive(int index){
	if(exclusive==1) return 0;
	int fd = pDevs[index].ufds.fd;
	debug("Device fd for exclusivity: %d", fd);
	int res = ioctl(fd, EVIOCGRAB, 1);
	if(res==0) exclusive=1;
	debug("Exclusivity result: %d", res);
	return res;
}

int releaseExclusive(int index){
	if(exclusive==0) return 0;
	int fd = pDevs[index].ufds.fd;
	debug("Device fd for exclusivity release: %d", fd);
	int res = ioctl(fd, EVIOCGRAB, NULL);
	debug("Exclusivity result: %d", res);
	if(res==0) exclusive=0;
	return res;
}

static int scan_dir(const char *dirname)
{
	nDevsCount = 0;
    char devname[PATH_MAX];
    char *filename;
    DIR *dir;
    struct dirent *de;
    dir = opendir(dirname);
    if(dir == NULL)
        return -1;
    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.' &&
           (de->d_name[1] == '\0' ||
        		   (de->d_name[1] == '.' && de->d_name[2] == '\0')
           )
        )
            continue;
        if(strstr(de->d_name, "js")){
        	debug("scandir","found js");
        	continue;
        }
        strcpy(filename, de->d_name);
		debug("scan_dir:prepare to open:%s", devname);
		// add new filename to our structure: devname
		struct typedev *new_pDevs = realloc(pDevs, sizeof(pDevs[0]) * (nDevsCount + 1));
		if(new_pDevs == NULL) {
			debug("out of memory");
			return -1;
		}
		pDevs = new_pDevs;

		struct pollfd *new_ufds = realloc(ufds, sizeof(ufds[0]) * (nDevsCount + 1));
		if(new_ufds == NULL) {
			debug("out of memory");
			return -1;
		}
		ufds = new_ufds;
		ufds[nDevsCount].events = POLLIN;

		pDevs[nDevsCount].ufds.events = POLLIN;
		pDevs[nDevsCount].device_path = strdup(devname);

        nDevsCount++;
    }
    closedir(dir);
    return 0;
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_CreateMapDevice(JNIEnv *env, jobject thiz, jint x, jint y){
	debug("Creating mapper with %d,%d resolution",x,y);
	createDevice(x,y);
}
jint Java_com_chrisjdowd_empio_mapper_KMEE_intEnableDebug( JNIEnv* env,jobject thiz, jint enable ) {
	g_debug = enable;
	return g_debug;
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_SendEvent(JNIEnv* env,jobject thiz, jint index, uint16_t type, uint16_t code, int32_t value) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1) return -1;
	int fd = pDevs[index].ufds.fd;
	struct uinput_event event;
	int len;

	if (fd <= fileno(stderr)) return;
	memset(&event, 0, sizeof(event));
	event.type = type;
	event.code = code;
	event.value = value;
	len = write(fd, &event, sizeof(event));
	debug("SEND %d,%d,%d)", type, code, value);

}



jint Java_com_chrisjdowd_empio_mapper_KMEE_ScanFiles( JNIEnv* env,jobject thiz ) {
	int res = scan_dir(device_path);
	if(res < 0) {
		debug("scan dir failed for %s:", device_path);
		return -1;
	}

	return nDevsCount;
}

jstring Java_com_chrisjdowd_empio_mapper_KMEE_getDevicePath( JNIEnv* env,jobject thiz, jint index) {
	return (*env)->NewStringUTF(env, pDevs[index].device_path);
}
jstring Java_com_chrisjdowd_empio_mapper_KMEE_getDeviceName( JNIEnv* env,jobject thiz, jint index) {
	if (pDevs[index].device_name == NULL) return NULL;
	else return (*env)->NewStringUTF(env, pDevs[index].device_name);
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_OpenDevice( JNIEnv* env,jobject thiz, jint index ) {
	return open_device(index);
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_CloseDevice( JNIEnv* env,jobject thiz, jint index ) {
	return remove_device(index);
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_GetExclusiveControl(JNIEnv* env, jobject thiz, jint index){
	return getExclusive(index);
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_ReleaseExclusiveControl(JNIEnv* env, jobject thiz, jint index){
	return releaseExclusive(index);
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_PollDevice( JNIEnv* env,jobject thiz, jint index) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1) return -1;
	int pollres = poll(ufds, nDevsCount, -1);
	if(ufds[index].revents) {
		if(ufds[index].revents & POLLIN) {
			int res = read(ufds[index].fd, &event, sizeof(event));
			if(res < (int)sizeof(event)) {
				return 1;
			}
			else return 0;
		}
	}
	return -1;
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_getType( JNIEnv* env,jobject thiz ) {
	return event.type;
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_getCode( JNIEnv* env,jobject thiz ) {
	return event.code;
}

jint Java_com_chrisjdowd_empio_mapper_KMEE_getValue( JNIEnv* env,jobject thiz ) {
	return event.value;
}
