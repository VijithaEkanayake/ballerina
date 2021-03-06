/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.runtime.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

import static org.ballerinalang.util.BLangConstants.BALLERINA_RUNTIME_PKG;

/**
 * Extern function to get authentication context.
 *
 * @since 0.970.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "runtime",
        functionName = "getInvocationContext",
        returnType = {@ReturnType(type = TypeKind.OBJECT, structType = "InvocationContext",
                structPackage = BALLERINA_RUNTIME_PKG)},
        isPublic = true
)
public class GetInvocationContext extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        context.setReturnValues(InvocationContextUtils.getInvocationContextStruct(context));
    }
}
