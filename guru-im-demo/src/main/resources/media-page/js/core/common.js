// resources/media-page/js/common.js
class Logger {
    static debug(...args) {
        this._log('debug', ...args);
    }

    static info(...args) {
        this._log('info', ...args);
    }

    static warn(...args) {
        this._log('warn', ...args);
    }

    static error(...args) {
        this._log('error', ...args);
    }

    static _log(level, ...args) {
        const timestamp = new Date().toISOString();
        const message = `[${timestamp}] [${level.toUpperCase()}] ${args.join(' ')}`;

        // 发送到Java端
        if (window.sendLog) {
            window.sendLog(level, message);
        }

        // 控制台输出
        if (console[level]) {
            console[level](...args);
        }
    }
}

class EventEmitter {
    constructor() {
        this.events = {};
    }

    on(event, listener) {
        if (!this.events[event]) {
            this.events[event] = [];
        }
        this.events[event].push(listener);
    }

    off(event, listener) {
        if (!this.events[event]) return;

        const index = this.events[event].indexOf(listener);
        if (index > -1) {
            this.events[event].splice(index, 1);
        }
    }

    emit(event, ...args) {
        if (!this.events[event]) return;

        this.events[event].forEach(listener => {
            try {
                listener(...args);
            } catch (error) {
                Logger.error('Event listener error:', error);
            }
        });
    }
}

class Utils {
    static generateId() {
        return Math.random().toString(36).substr(2, 9);
    }

    static async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    static formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    static createAvatar(name) {
        return name ? name.charAt(0).toUpperCase() : 'U';
    }

    // 添加日志方法到Utils，作为Logger的别名
    static log(...args) {
        Logger.info(...args);
    }

    static debug(...args) {
        Logger.debug(...args);
    }

    static info(...args) {
        Logger.info(...args);
    }

    static warn(...args) {
        Logger.warn(...args);
    }

    static error(...args) {
        Logger.error(...args);
    }
}

// 全局日志函数
window.sendLog = function(level, message) {
    try {
        if (window.cefQuery) {
            window.cefQuery({
                request: JSON.stringify({
                    method: 'log',
                    data: { level, message }
                }),
                onSuccess: function(response) {},
                onFailure: function(errorCode, errorMessage) {
                    console.error('CEF log failed:', errorCode, errorMessage);
                }
            });
        }
    } catch (error) {
        console.error('Send log failed:', error);
    }
};

// 导出到全局
window.Logger = Logger;
window.EventEmitter = EventEmitter;
window.Utils = Utils;