package com.acertainsupplychain.utils;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.acertainsupplychain.ItemQuantity;

public class OrderMangerFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        if(record.getParameters() == null){
            return "";
        }
        OrderManagerFormatterParameter result = 
                (OrderManagerFormatterParameter) record.getParameters()[0];
        StringBuffer sb = new StringBuffer(1000);
        String prefix;
        
        sb.append(result.getWorkflowId());
        sb.append('\t');
        sb.append(result.getSupplierId());
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
        sb.append('\t');
        sb.append(result.getStatus());
        sb.append('\n');

        return sb.toString();
    }
    
    public String getHead(Handler h) {
        return "workflowId\tsupplierId\titemIds\tquantities\tstatus\n";
    }

}
