package org.ballerinalang.langserver.common.utils;

import org.ballerinalang.langserver.common.UtilSymbolKeys;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSServiceOperationContext;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BEndpointVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for filtering the symbols from completion context and symbol information lists.
 */
public class FilterUtils {

    /**
     * Get the invocations and fields against an identifier (functions, actions and fields).
     *
     * @param context               Text Document Service context (Completion Context)
     * @param variableName          Variable name to evaluate against (Can be package alias or defined variable)
     * @param delimiter             delimiter String (. or ->)
     * @return {@link ArrayList}    List of filtered symbol info
     */
    public static List<SymbolInfo> getInvocationAndFieldSymbolsOnVar(LSServiceOperationContext context,
                                                                     String variableName, String delimiter) {
        ArrayList<SymbolInfo> resultList = new ArrayList<>();
        List<SymbolInfo> symbols = context.get(CompletionKeys.VISIBLE_SYMBOLS_KEY);
        SymbolTable symbolTable = context.get(DocumentServiceKeys.SYMBOL_TABLE_KEY);
        SymbolInfo variable = getVariableByName(variableName, symbols);

        if (variable == null) {
            return resultList;
        }

        BType bType = variable.getScopeEntry().symbol.getType();
        BSymbol bVarSymbol = variable.getScopeEntry().symbol;

        if (variable.getScopeEntry().symbol instanceof BEndpointVarSymbol
                && delimiter.equals(UtilSymbolKeys.ACTION_INVOCATION_SYMBOL_KEY)
                && ((BEndpointVarSymbol) bVarSymbol).getClientFunction.type instanceof BInvokableType) {
            resultList.addAll(getEndpointActions((BEndpointVarSymbol) variable.getScopeEntry().symbol));
        } else if (delimiter.equals(UtilSymbolKeys.DOT_SYMBOL_KEY)) {
            String builtinPkgName = symbolTable.builtInPackageSymbol.pkgID.name.getValue();
            String currentPkgName = context.get(DocumentServiceKeys.CURRENT_PACKAGE_NAME_KEY);
            Map<Name, Scope.ScopeEntry> entries = new HashMap<>();
            String packageID = getPackageIDForBType(bType).getName().getValue();
            String bTypeValue = bType.toString();

            // Extract the package symbol. This is used to extract the entries of the particular package
            SymbolInfo packageSymbolInfo = symbols.stream().filter(item -> {
                Scope.ScopeEntry scopeEntry = item.getScopeEntry();
                return (scopeEntry.symbol instanceof BPackageSymbol)
                        && scopeEntry.symbol.pkgID.name.getValue().equals(packageID);
            }).findFirst().orElse(null);

            if (packageID.equals(builtinPkgName)) {
                // If the packageID is ballerina/builtin, we extract entries of builtin package
                entries = symbolTable.builtInPackageSymbol.scope.entries;
            } else if (packageSymbolInfo == null && packageID.equals(currentPkgName)) {
                entries = getScopeEntries(bType, context);
            } else if (packageSymbolInfo != null) {
                // If the package exist, we extract particular entries from package
                entries = packageSymbolInfo.getScopeEntry().symbol.scope.entries;
            }

            entries.forEach((name, scopeEntry) -> {
                if (scopeEntry.symbol instanceof BInvokableSymbol && scopeEntry.symbol.owner != null) {
                    String symbolBoundedName = scopeEntry.symbol.owner.toString();

                    if (symbolBoundedName.equals(bTypeValue)) {
                        // TODO: Need to handle the name in a proper manner
                        String[] nameComponents = name.toString().split("\\.");
                        SymbolInfo actionFunctionSymbol =
                                new SymbolInfo(nameComponents[nameComponents.length - 1], scopeEntry);
                        resultList.add(actionFunctionSymbol);
                    }
                } else if ((scopeEntry.symbol instanceof BTypeSymbol)
                        && (SymbolKind.OBJECT.equals(scopeEntry.symbol.kind)
                        || SymbolKind.RECORD.equals(scopeEntry.symbol.kind))
                        && bTypeValue.equals(scopeEntry.symbol.type.toString())) {
                    // Get the struct fields
                    Map<Name, Scope.ScopeEntry> fields = scopeEntry.symbol.scope.entries;
                    fields.forEach((fieldName, fieldScopeEntry) -> {
                        resultList.add(new SymbolInfo(fieldName.getValue(), fieldScopeEntry));
                    });
                }
            });

            CommonUtil.populateIterableOperations(variable, resultList);
        } else if (delimiter.equals(UtilSymbolKeys.PKG_DELIMITER_KEYWORD)) {
            SymbolInfo packageSymbol = symbols.stream().filter(item -> {
                Scope.ScopeEntry scopeEntry = item.getScopeEntry();
                return item.getSymbolName().equals(variableName) && scopeEntry.symbol instanceof BPackageSymbol;
            }).findFirst().orElse(null);

            Map<Name, Scope.ScopeEntry> scopeEntryMap = packageSymbol.getScopeEntry().symbol.scope.entries;
            resultList.addAll(loadActionsFunctionsAndTypesFromScope(scopeEntryMap));
        }

        return resultList;
    }

    /**
     * Get the actions defined over and endpoint.
     * @param bEndpointVarSymbol    Endpoint variable symbol to evaluate
     * @return {@link List}         List of extracted actions as Symbol Info
     */
    public static List<SymbolInfo> getEndpointActions(BEndpointVarSymbol bEndpointVarSymbol) {
        List<SymbolInfo> endpointActions = new ArrayList<>();
        BType getClientFuncType = bEndpointVarSymbol.getClientFunction.type;
        BType boundType = ((BInvokableType) getClientFuncType).retType;
        boundType.tsymbol.scope.entries.forEach((name, scopeEntry) -> {
            String[] nameComponents = name.toString().split("\\.");
            if (scopeEntry.symbol instanceof BInvokableSymbol
                    && !nameComponents[nameComponents.length - 1].equals(UtilSymbolKeys.NEW_KEYWORD_KEY)) {
                SymbolInfo actionFunctionSymbol =
                        new SymbolInfo(nameComponents[nameComponents.length - 1], scopeEntry);
                endpointActions.add(actionFunctionSymbol);
            }
        });

        return endpointActions;
    }
    
    ///////////////////////////
    ///// Private Methods /////
    ///////////////////////////

    /**
     * Get the scope entries.
     *
     * @param bType         BType
     * @param completionCtx Completion context
     * @return {@link Map} Scope entries map
     */
    private static Map<Name, Scope.ScopeEntry> getScopeEntries(BType bType, LSServiceOperationContext completionCtx) {
        HashMap<Name, Scope.ScopeEntry> returnMap = new HashMap<>();
        completionCtx.get(CompletionKeys.VISIBLE_SYMBOLS_KEY)
                .forEach(symbolInfo -> {
                    if ((symbolInfo.getScopeEntry().symbol instanceof BTypeSymbol
                            && symbolInfo.getScopeEntry().symbol.getType() != null
                            && symbolInfo.getScopeEntry().symbol.getType().toString().equals(bType.toString()))
                            || symbolInfo.getScopeEntry().symbol instanceof BInvokableSymbol) {
                        returnMap.put(symbolInfo.getScopeEntry().symbol.getName(), symbolInfo.getScopeEntry());
                    }
                });

        return returnMap;
    }

    /**
     * Get the variable symbol info by the name.
     *
     * @param name    name of the variable
     * @param symbols list of symbol info
     * @return {@link SymbolInfo}   Symbol Info extracted
     */
    private static SymbolInfo getVariableByName(String name, List<SymbolInfo> symbols) {
        return symbols.stream()
                .filter(symbolInfo -> symbolInfo.getSymbolName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    private static PackageID getPackageIDForBType(BType bType) {
        if (bType instanceof BArrayType) {
            return  ((BArrayType) bType).eType.tsymbol.pkgID;
        }
        return bType.tsymbol.pkgID;
    }

    private static List<SymbolInfo> loadActionsFunctionsAndTypesFromScope(Map<Name, Scope.ScopeEntry> entryMap) {
        List<SymbolInfo> actionFunctionList = new ArrayList<>();
        entryMap.forEach((name, value) -> {
            BSymbol symbol = value.symbol;
            if (((symbol instanceof BInvokableSymbol && ((BInvokableSymbol) symbol).receiverSymbol == null)
                    || (symbol instanceof BTypeSymbol && !(symbol instanceof BPackageSymbol))
                    || symbol instanceof BVarSymbol) && (symbol.flags & Flags.PUBLIC) == Flags.PUBLIC) {
                SymbolInfo entry = new SymbolInfo(name.toString(), value);
                actionFunctionList.add(entry);
            }
        });

        return actionFunctionList;
    }
}
