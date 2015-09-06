## ObjectModel.java


```
@Table(name = "tabela", endpoint = "")
public class ObjectModel extends RotLiteObject {

    public ObjectModel(Context context) {
        super(context, ObjectModel.class);
    }

}
```
## AndroidManifest.xml ##

```
<application>
 ...
 
<meta-data
            android:name="rotlite_dbname"
            android:value="rotlite" />
<meta-data
    android:name="rotlite_server"
    android:value="WEBSERVICE_URL" />
    
 ...
</application>
```

## Inserindo dados ##


```
ObjectModel model = new ObjectModel(context);
model.put("column","value");
model.put("column2", 1);

try {
    model.saveLocal();
}catch(Exception e) {
    e.printStackTrace();
}
```

## Inserindo dados na WEB e no dispositivo ##


```
ObjectModel model = new ObjectModel(context);
model.put("column","value");
model.put("column2", 1);

try {
    model.saveLocal();
    model.saveWeb(new Callback() {

                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.e("RotLite", "Failure request: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        if (response.isSuccessful()) {

                            String responseStr = response.body().string();
                            Log.v("RotLite", "Response: " + responseStr);

                        } else {
                            Log.e("RotLite", "Response error: " + response.message());
                        }
                    }

                });

}catch(Exception e) {
    e.printStackTrace();
}
```

## Buscando todos dados da tabela ##

        ObjectModel model = new ObjectModel(context);
        model.find(new RotLiteCallback<List<RotLiteObject>>() {
            @Override
            public void done(List<RotLiteObject> list) {

                for (RotLiteObject obj : list) {

                    Log.v("MainActivity", "Full object data: " + obj.jsonString());

                }

            }

            @Override
            public void error(Exception e) {
                Log.e("MainActivity", e.getMessage());
            }
        });

## Buscando um objeto pelo ID ##


```
ObjectModel model = new ObjectModel(context);
model.getById(10);
model.find(...);
```


## Atualizando um objeto pelo ID ##


```
ObjectModel model = new ObjectModel(context);
model.getById(10);
model.put("column", "newvalue");
if (model.update()) {
 //atualizado
}else{
 //erro ao atualizar
}
```


## Atualizando vários dados da tabela ##


```
ObjectModel model = new ObjectModel(context);
model.where("column = 'value'");
if (model.update()) {
  //Tudo atualizado!
}else{
  //Nada atualizado
}
```


## Atualizando vários dados e identificando erros caso não atualize ##


```
ObjectModel model = new ObjectModel(context);
model.where("column = 'value'");
model.find(new RotLiteCallback<List<RotLiteObject>>() {
               @Override
               public void done(List<RotLiteObject> list) {

                   for (RotLiteObject obj : list) {

                       obj.put("column", "new_value");
                       if (obj.update()) {
                       
                           //atualizado!
                       
                       }else{

                           //não atualizado

                       }

                   }

               }

               @Override
               public void error(Exception e) {
                   Log.e("MainActivity", e.getMessage());
               }
          });
```



## Removendo objetos ##


```
ObjectModel model = new ObjectModel(context);
model.getById(10);
if (model.delete()) {
 //removido
}else{
 //erro ao remover
}
```


## Limitando resultados ##


```
ObjectModel model = new ObjectModel(context);
model.limit(10);
//ou
model.limit(5,20);
model.find(...);
```


## Ordenando resultados ##

```
ObjectModel model = new ObjectModel(context);
model.order("column ASC");
model.find(...);
```
