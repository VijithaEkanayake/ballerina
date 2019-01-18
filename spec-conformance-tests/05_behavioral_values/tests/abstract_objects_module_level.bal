// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import ballerina/test;

@test:Config {}
function testAbstractObjectTypeDescriptorInSameModule() {
    ObjReferenceToAbstractClientObject abstractClientObj2 = new("string", 12, 100.0);
    ObjReferenceToAbstractObject abstractObj = new("string", 12, 100.0);

    test:assertEquals(abstractObj.publicStringField, "string", msg = "expected object public field to be accessible");
    test:assertEquals(abstractObj.getPrivateField(), 12,
        msg = "expected object private field to be accessible via object method");
    test:assertEquals(abstractObj.defaultVisibilityFloatField, 100.0,
        msg = "expected object default visibilty field to be accessible");

    test:assertEquals(abstractObj.defaultVisibiltyMethodDecl("argOne", 50), (),
        msg = "expected object default visibility method to be accessible");
    test:assertEquals(abstractObj.defaultVisibiltyMethodOutsideDecl("argOne", 25), (),
        msg = "expected object default visibility method declared outside to be accessible");
    test:assertEquals(abstractObj.publicMethodDecl("argOne", 125, 25), 325.0,
        msg = "expected object public visibility method to be accessible");


    test:assertEquals(abstractClientObj2.publicStringField, "string",
        msg = "expected client object public field to be accessible");
    test:assertEquals(abstractClientObj2.getPrivateField(), 12,
        msg = "expected client object private field to be accessible via object method");
    test:assertEquals(abstractClientObj2.defaultVisibilityFloatField, 100.0,
        msg = "expected client object default visibilty field to be accessible");

    test:assertEquals(abstractClientObj2.defaultVisibiltyMethodDecl("argOne", 50), (),
        msg = "expected client object default visibility method to be accessible");
    test:assertEquals(abstractClientObj2.defaultVisibiltyMethodOutsideDecl("argOne", 25), (),
        msg = "expected client object default visibility method declared outside to be accessible");
    _ = abstractClientObj2->defaultVisibiltyRemoteMethodDecl("argOne", 25);
    test:assertEquals(abstractClientObj2.publicMethodDecl("argOne", 125, 25), 350.0,
        msg = "expected client object public visibility method to be accessible");
    var result = abstractClientObj2->remoteMethodOutsideDecl("argOne", 125);
    test:assertEquals(result, 475.0,
        msg = "expected client object public visibility remote method declared outside to be accessible");
    result = abstractClientObj2->publicRemoteMethodDecl("argOne", 125, 50);
    test:assertEquals(result, 650.0,
        msg = "expected client object public visibility remote method declared outside to be accessible");
}
