package com.acertainsupplychain.utils;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.acertainsupplychain.ItemQuantity;

public class ItemSupplierFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        if(record.getParameters() == null){
            return "";
        }
        ItemSupplierFormatterParameter result = 
                (ItemSupplierFormatterParameter) record.getParameters()[0];
        StringBuffer sb = new StringBuffer(1000);
        String prefix;
        
        String managerId = ".";
        if(result.getOrderManagerId() != null){
            managerId = String.valueOf(result.getOrderManagerId());
        }
        String workflowId = ".";
        if(result.getWorkflowId() != null){
            workflowId = String.valueOf(result.getWorkflowId());
        }
        
        sb.append(managerId);
        sb.append('\t');
        sb.append(workflowId);
        sb.append('\t');
        prefix = "";
        for(ItemQuantity item : result.getItems()){
            sb.append(prefix);
            prefix = ",";
            sb.append(item.getItemId());
        }
        sb.append('\t');
        prefix = "";
        for(ItemQuantity item : result.getItems()){
            sb.append(prefix);
            prefix = ",";
            sb.append(item.getQuantity());
        }
        sb.append('\n');

        return sb.toString();
    }
    
    public String getHead(Handler h) {
        return "orderManagerId\tworkflowId\titemIds\tquantities\n";
    }

}
