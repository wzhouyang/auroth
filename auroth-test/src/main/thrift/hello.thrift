namespace java com.wzy.auroth.test.consul.register.service

include "user.thrift"

service HelloService {
    user.User getUser(1: i64 id);

    string testString(1: i64 id);
}