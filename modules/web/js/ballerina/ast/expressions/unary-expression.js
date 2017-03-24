/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import _ from 'lodash';
import Expression from './expression';

/**
 * Construct for UnaryExpression
 * @param {Object} args - Arguments to create the Unary Expression
 * @constructor
 * @augments Expression
 * */
class UnaryExpression extends Expression {
    constructor(args) {
        super('UnaryExpression');
        this._operator = _.get(args, 'operator');
    }

    /**
     * setting parameters from json
     * @param {Object} jsonNode to initialize from
     * */
    initFromJson(jsonNode) {
        this.setExpression(this.generateExpressionString(jsonNode), {doSilently: true});
    }

    /**
     * Generates the binary expression as a string.
     * @param {Object} jsonNode - A node explaining the structure of binary expression.
     * @return {String} - Arguments as a string
     * @private
     * */
    generateExpressionString(jsonNode) {
        var self = this;
        var expString = "";
        expString += this.getOperator();
        for (var itr = 0; itr < jsonNode.children.length; itr++) {
            var childJsonNode = jsonNode.children[itr];
            var child = self.getFactory().createFromJson(childJsonNode);
            child.initFromJson(childJsonNode);
            expString += child.getExpression();
        }
        return expString;
    }

    /**
     * Get the operator.
     * @return {String} Operator
     * */
    getOperator() {
        return this._operator;
    }
}

export default UnaryExpression;
