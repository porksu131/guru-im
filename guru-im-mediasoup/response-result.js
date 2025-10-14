class ResponseResult {
    static SUCCESS = 200;
    static FAIL = 500;

    constructor(code, msg, data = null) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    static ok() {
        return new ResponseResult(this.SUCCESS, "success");
    }

    static ok(data) {
        return new ResponseResult(this.SUCCESS, "success", data);
    }

    static okMsg(msg) {
        return new ResponseResult(this.SUCCESS, msg);
    }

    static okData(msg, data) {
        return new ResponseResult(this.SUCCESS, msg, data);
    }

    static fail() {
        return new ResponseResult(this.FAIL, "failure");
    }

    static fail(msg) {
        return new ResponseResult(this.FAIL, msg);
    }

    static failCode(code, msg) {
        return new ResponseResult(code, msg);
    }

    static failData(msg, data) {
        return new ResponseResult(this.FAIL, msg, data);
    }
}

module.exports = ResponseResult;