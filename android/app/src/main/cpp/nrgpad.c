#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <unistd.h>

#define PACKET_SIZE 8
#define PKT_KEY 1
#define PKT_MOUSE_MOVE 2
#define PKT_MOUSE_BUTTON 3
#define PKT_PAD_BUTTON 4
#define PKT_PAD_HAT 5
#define PKT_PAD_AXIS 6
#define PKT_RESET 7
#define PKT_MODE 8

#define PAD_CROSS 0
#define PAD_CIRCLE 1
#define PAD_SQUARE 2
#define PAD_TRIANGLE 3
#define PAD_L1 4
#define PAD_R1 5
#define PAD_L2 6
#define PAD_R2 7
#define PAD_SELECT 8
#define PAD_START 9

static int keyboard_fd = -1;
static int mouse_fd = -1;
static int gamepad_fd = -1;
static int keyboard_down[768];
static int pad_down[10];
static int hat_x = 0;
static int hat_y = 0;
static int axis_x = 0;
static int axis_y = 0;

static void log_error(const char *message) {
    fprintf(stderr, "nrgpad: %s: %s\n", message, strerror(errno));
    fflush(stderr);
}

static int write_full(int fd, const void *data, size_t size) {
    const unsigned char *p = (const unsigned char *)data;
    while (size > 0) {
        ssize_t n = write(fd, p, size);
        if (n < 0) {
            if (errno == EINTR) continue;
            return -1;
        }
        p += (size_t)n;
        size -= (size_t)n;
    }
    return 0;
}

static int read_full(int fd, unsigned char *data, size_t size) {
    size_t got = 0;
    while (got < size) {
        ssize_t n = read(fd, data + got, size - got);
        if (n == 0) return got == 0 ? 0 : -1;
        if (n < 0) {
            if (errno == EINTR) continue;
            return -1;
        }
        got += (size_t)n;
    }
    return 1;
}

static int emit_event(int fd, unsigned short type, unsigned short code, int value) {
    struct input_event event;
    memset(&event, 0, sizeof(event));
    gettimeofday(&event.time, NULL);
    event.type = type;
    event.code = code;
    event.value = value;
    return write_full(fd, &event, sizeof(event));
}

static int emit_syn(int fd) {
    return emit_event(fd, EV_SYN, SYN_REPORT, 0);
}

static int open_uinput(void) {
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) fd = open("/dev/input/uinput", O_WRONLY | O_NONBLOCK);
    return fd;
}

static int create_device(int fd, const char *name, int vendor, int product,
                         const unsigned short *keys, size_t key_count,
                         const unsigned short *rels, size_t rel_count,
                         const unsigned short *abss, size_t abs_count,
                         int pointer_property) {
    struct uinput_user_dev device;
    size_t i;
    if (ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0) return -1;
    if (key_count > 0 && ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0) return -1;
    if (rel_count > 0 && ioctl(fd, UI_SET_EVBIT, EV_REL) < 0) return -1;
    if (abs_count > 0 && ioctl(fd, UI_SET_EVBIT, EV_ABS) < 0) return -1;
    for (i = 0; i < key_count; i++) if (ioctl(fd, UI_SET_KEYBIT, keys[i]) < 0) return -1;
    for (i = 0; i < rel_count; i++) if (ioctl(fd, UI_SET_RELBIT, rels[i]) < 0) return -1;
    for (i = 0; i < abs_count; i++) if (ioctl(fd, UI_SET_ABSBIT, abss[i]) < 0) return -1;
#ifdef UI_SET_PROPBIT
    if (pointer_property) ioctl(fd, UI_SET_PROPBIT, INPUT_PROP_POINTER);
#else
    (void)pointer_property;
#endif
    memset(&device, 0, sizeof(device));
    strncpy(device.name, name, UINPUT_MAX_NAME_SIZE - 1);
    device.id.bustype = BUS_VIRTUAL;
    device.id.vendor = (unsigned short)vendor;
    device.id.product = (unsigned short)product;
    device.id.version = 1;
    for (i = 0; i < abs_count; i++) {
        if (abss[i] == ABS_X || abss[i] == ABS_Y) {
            device.absmin[abss[i]] = -32767;
            device.absmax[abss[i]] = 32767;
            device.absfuzz[abss[i]] = 0;
            device.absflat[abss[i]] = 0;
        } else if (abss[i] == ABS_HAT0X || abss[i] == ABS_HAT0Y) {
            device.absmin[abss[i]] = -1;
            device.absmax[abss[i]] = 1;
            device.absfuzz[abss[i]] = 0;
            device.absflat[abss[i]] = 0;
        } else if (abss[i] == ABS_Z || abss[i] == ABS_RZ) {
            device.absmin[abss[i]] = 0;
            device.absmax[abss[i]] = 255;
            device.absfuzz[abss[i]] = 0;
            device.absflat[abss[i]] = 0;
        }
    }
    if (write_full(fd, &device, sizeof(device)) < 0) return -1;
    if (ioctl(fd, UI_DEV_CREATE) < 0) return -1;
    usleep(120000);
    return 0;
}

static int setup_keyboard(void) {
    static const unsigned short keys[] = {
        KEY_ESC, KEY_ENTER, KEY_HOME, KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT,
        KEY_VOLUMEDOWN, KEY_VOLUMEUP, KEY_PLAYPAUSE, KEY_BACK, KEY_MENU,
        KEY_PAGEUP, KEY_PAGEDOWN
    };
    keyboard_fd = open_uinput();
    if (keyboard_fd < 0) return -1;
    if (create_device(keyboard_fd, "Nokia Bluetooth Remote", 0x0001, 0x1001,
                      keys, sizeof(keys) / sizeof(keys[0]), NULL, 0, NULL, 0, 0) < 0) return -1;
    return 0;
}

static int setup_mouse(void) {
    static const unsigned short keys[] = { BTN_LEFT, BTN_RIGHT, BTN_MIDDLE };
    static const unsigned short rels[] = { REL_X, REL_Y, REL_WHEEL, REL_HWHEEL };
    mouse_fd = open_uinput();
    if (mouse_fd < 0) return -1;
    if (create_device(mouse_fd, "Nokia Bluetooth Mouse", 0x0001, 0x1002,
                      keys, sizeof(keys) / sizeof(keys[0]),
                      rels, sizeof(rels) / sizeof(rels[0]), NULL, 0, 1) < 0) return -1;
    return 0;
}

static int setup_gamepad(void) {
    static const unsigned short keys[] = {
        BTN_GAMEPAD, BTN_SOUTH, BTN_EAST, BTN_WEST, BTN_NORTH,
        BTN_TL, BTN_TR, BTN_TL2, BTN_TR2, BTN_SELECT, BTN_START
    };
    static const unsigned short abss[] = { ABS_X, ABS_Y, ABS_HAT0X, ABS_HAT0Y, ABS_Z, ABS_RZ };
    gamepad_fd = open_uinput();
    if (gamepad_fd < 0) return -1;
    if (create_device(gamepad_fd, "Nokia Bluetooth Gamepad", 0x0001, 0x1003,
                      keys, sizeof(keys) / sizeof(keys[0]), NULL, 0,
                      abss, sizeof(abss) / sizeof(abss[0]), 0) < 0) return -1;
    return 0;
}

static int pad_key_code(int button) {
    switch (button) {
        case PAD_CROSS: return BTN_SOUTH;
        case PAD_CIRCLE: return BTN_EAST;
        case PAD_SQUARE: return BTN_WEST;
        case PAD_TRIANGLE: return BTN_NORTH;
        case PAD_L1: return BTN_TL;
        case PAD_R1: return BTN_TR;
        case PAD_L2: return BTN_TL2;
        case PAD_R2: return BTN_TR2;
        case PAD_SELECT: return BTN_SELECT;
        case PAD_START: return BTN_START;
        default: return -1;
    }
}

static void reset_inputs(void) {
    int i;
    if (keyboard_fd >= 0) {
        for (i = 0; i < (int)(sizeof(keyboard_down) / sizeof(keyboard_down[0])); i++) {
            if (keyboard_down[i]) emit_event(keyboard_fd, EV_KEY, (unsigned short)i, 0);
            keyboard_down[i] = 0;
        }
        emit_syn(keyboard_fd);
    }
    if (mouse_fd >= 0) {
        emit_event(mouse_fd, EV_KEY, BTN_LEFT, 0);
        emit_event(mouse_fd, EV_KEY, BTN_RIGHT, 0);
        emit_syn(mouse_fd);
    }
    if (gamepad_fd >= 0) {
        for (i = 0; i < 10; i++) {
            int code = pad_key_code(i);
            if (code >= 0 && pad_down[i]) emit_event(gamepad_fd, EV_KEY, (unsigned short)code, 0);
            pad_down[i] = 0;
        }
        hat_x = hat_y = axis_x = axis_y = 0;
        emit_event(gamepad_fd, EV_ABS, ABS_HAT0X, 0);
        emit_event(gamepad_fd, EV_ABS, ABS_HAT0Y, 0);
        emit_event(gamepad_fd, EV_ABS, ABS_X, 0);
        emit_event(gamepad_fd, EV_ABS, ABS_Y, 0);
        emit_event(gamepad_fd, EV_ABS, ABS_Z, 0);
        emit_event(gamepad_fd, EV_ABS, ABS_RZ, 0);
        emit_syn(gamepad_fd);
    }
}

static void handle_packet(const unsigned char *packet) {
    int type = packet[3];
    int code = (int16_t)(((unsigned int)packet[4] << 8) | packet[5]);
    int value = (int16_t)(((unsigned int)packet[6] << 8) | packet[7]);
    switch (type) {
        case PKT_KEY:
            if (keyboard_fd >= 0 && code >= 0 && code < (int)(sizeof(keyboard_down) / sizeof(keyboard_down[0]))) {
                emit_event(keyboard_fd, EV_KEY, (unsigned short)code, value ? 1 : 0);
                emit_syn(keyboard_fd);
                keyboard_down[code] = value ? 1 : 0;
            }
            break;
        case PKT_MOUSE_MOVE:
            if (mouse_fd >= 0) {
                if (code != 0) emit_event(mouse_fd, EV_REL, REL_X, code);
                if (value != 0) emit_event(mouse_fd, EV_REL, REL_Y, value);
                emit_syn(mouse_fd);
            }
            break;
        case PKT_MOUSE_BUTTON:
            if (mouse_fd >= 0) {
                unsigned short button = code == 2 ? BTN_RIGHT : code == 3 ? BTN_MIDDLE : BTN_LEFT;
                emit_event(mouse_fd, EV_KEY, button, value ? 1 : 0);
                emit_syn(mouse_fd);
            }
            break;
        case PKT_PAD_BUTTON:
            if (gamepad_fd >= 0 && code >= 0 && code < 10) {
                int key = pad_key_code(code);
                if (key >= 0) {
                    emit_event(gamepad_fd, EV_KEY, (unsigned short)key, value ? 1 : 0);
                    if (code == PAD_L2) emit_event(gamepad_fd, EV_ABS, ABS_Z, value ? 255 : 0);
                    if (code == PAD_R2) emit_event(gamepad_fd, EV_ABS, ABS_RZ, value ? 255 : 0);
                    emit_syn(gamepad_fd);
                    pad_down[code] = value ? 1 : 0;
                }
            }
            break;
        case PKT_PAD_HAT:
            if (gamepad_fd >= 0) {
                if (code == 0) { hat_x = value < 0 ? -1 : value > 0 ? 1 : 0; emit_event(gamepad_fd, EV_ABS, ABS_HAT0X, hat_x); }
                else if (code == 1) { hat_y = value < 0 ? -1 : value > 0 ? 1 : 0; emit_event(gamepad_fd, EV_ABS, ABS_HAT0Y, hat_y); }
                emit_syn(gamepad_fd);
            }
            break;
        case PKT_PAD_AXIS:
            if (gamepad_fd >= 0) {
                int v = value < -32767 ? -32767 : value > 32767 ? 32767 : value;
                if (code == 0) { axis_x = v; emit_event(gamepad_fd, EV_ABS, ABS_X, axis_x); }
                else if (code == 1) { axis_y = v; emit_event(gamepad_fd, EV_ABS, ABS_Y, axis_y); }
                emit_syn(gamepad_fd);
            }
            break;
        case PKT_RESET:
            reset_inputs();
            break;
        case PKT_MODE:
        default:
            break;
    }
}

static void destroy_device(int *fd) {
    if (*fd >= 0) {
        ioctl(*fd, UI_DEV_DESTROY);
        close(*fd);
        *fd = -1;
    }
}

int main(int argc, char **argv) {
    unsigned char packet[PACKET_SIZE];
    int result;
    (void)argc;
    (void)argv;

    if (setup_keyboard() < 0) { log_error("create keyboard /dev/uinput device"); goto fail; }
    if (setup_mouse() < 0) { log_error("create mouse /dev/uinput device"); goto fail; }
    if (setup_gamepad() < 0) { log_error("create gamepad /dev/uinput device"); goto fail; }
    printf("READY: virtual keyboard, mouse and gamepad created\n");
    fflush(stdout);

    for (;;) {
        result = read_full(STDIN_FILENO, packet, sizeof(packet));
        if (result == 0) break;
        if (result < 0) { log_error("read protocol packet"); break; }
        if (packet[0] != 0x4e || packet[1] != 0x52 || packet[2] != 1) {
            fprintf(stderr, "nrgpad: discarded invalid packet header\n");
            fflush(stderr);
            continue;
        }
        handle_packet(packet);
    }

    reset_inputs();
    destroy_device(&gamepad_fd);
    destroy_device(&mouse_fd);
    destroy_device(&keyboard_fd);
    printf("STOPPED: input devices released\n");
    fflush(stdout);
    return 0;

fail:
    reset_inputs();
    destroy_device(&gamepad_fd);
    destroy_device(&mouse_fd);
    destroy_device(&keyboard_fd);
    return 1;
}
