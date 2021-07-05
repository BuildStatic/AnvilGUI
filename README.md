# AnvilGUI [![Build Status](https://ci.codemc.io/job/WesJD/job/AnvilGUI/badge/icon)](https://ci.codemc.io/job/WesJD/job/AnvilGUI/)
Easily use anvil guis to get a user's input.

This project was made since there is no way to prompt users with an anvil input with the Spigot / Bukkit API. It requires interaction with NMS and that is a pain in plugins where users have different versions of the server running.

## Requirements
Java 8 and Bukkit / Spigot. Most server versions in the [Spigot Repository](https://hub.spigotmc.org/nexus/) are supported.

### My version isn't supported
If you are a developer, submit a pull request adding a wrapper module for your version. Otherwise, please create an issue
on the issues tab. 

## Usage

### As a dependency

AnvilGUI requires the usage of Maven or a Maven compatible build system. 
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.4</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <minimizeJar>true</minimizeJar>
                        <createDependencyeducedPom>false</createDependencyeducedPom>
                        <relocations>
                            <relocation>
                                <pattern>net.wesjd.anvilgui</pattern>
                                <shadedPattern>[YOUR_PLUGIN_PACKAGE].anvilgui</shadedPattern> <!-- Replace this -->
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<dependency>
    <groupId>net.wesjd</groupId>
    <artifactId>anvilgui</artifactId>
    <version>1.5.1-SNAPSHOT</version>
</dependency>

<repository>
    <id>codemc-snapshots</id>
    <url>https://repo.codemc.io/repository/maven-snapshots/</url>
</repository>
```

```groovy
plugins {
    id 'com.github.johnrengelman.shadow' version '7.0.0'
}

repositories {
    maven {
        url 'https://repo.codemc.io/repository/maven-snapshots/'
    }
}

dependencies {
    implementation 'net.wesjd:anvilgui:1.5.1-SNAPSHOT'
}

shadowJar {
    minimize()
    relocate 'net.wesjd.anvilgui', '[YOUR PLUGIN PACKAGE].anvilgui'
}
```

### In your plugin

The `AnvilGUI.Builder` class is how you build an AnvilGUI. 
The following methods allow you to modify various parts of the displayed GUI. Javadocs are available [here](http://docs.wesjd.net/AnvilGUI/).

#### `onClose(Consumer<Player>)` 
Takes a `Consumer<Player>` argument that is called when a player closes the anvil gui.
```java                                             
builder.onClose(player -> {                         
    player.sendMessage("You closed the inventory.");
});                                                 
``` 

#### `onComplete(BiFunction<Player, String, AnvilGUI.Response>)`  
Takes a `BiFunction<Player, String, AnvilGUI.Response>` argument. The BiFunction is called when a player clicks the output slot. 
The supplied string is what the player has inputted in the renaming field of the anvil gui. You must return an AnvilGUI.Response,
which can either be `close()`, `text(String)`, or `openInventory(Inventory)`. Returning `close()` will close the inventory; returning 
`text(String)` will keep the inventory open and put the supplied String in the renaming field; returning `openInventory(Inventory)`
will open the provided inventory, which is useful for when a user has finished their input in GUI menus.
```java                                                
builder.onComplete((player, text) -> {                 
    if(text.equalsIgnoreCase("you")) {                 
        player.sendMessage("You have magical powers!");
        return AnvilGUI.Response.close();              
    } else {                                           
        return AnvilGUI.Response.text("Incorrect.");   
    }                                                  
});                                                    
```                                                    

#### `preventClose()` 
Tells the AnvilGUI to prevent the user from pressing escape to close the inventory.
Useful for situations like password input to play.                                      
```java                     
builder.preventClose();     
```                         
     
#### `text(String)`
Takes a `String` that contains what the initial text in the renaming field should be set to.
```java                                           
builder.text("What is the meaning of life?");     
```  

#### `itemLeft(ItemStack)`
Takes a custom `ItemStack` to be placed in the left input slot.
```java                                              
ItemStack stack = new ItemStack(Material.IRON_SWORD);
ItemMeta meta = stack.getItemMeta();                 
meta.setLore(Arrays.asList("Sharp iron sword"));             
stack.setItemMeta(meta); 
builder.itemLeft(stack);        
```         

#### `onLeftInputClick(Consumer<Player>)`
Takes a `Consumer<Player>` to be executed when the item in the left input slot is clicked.
```java                                              
builder.onLeftInputClick(player -> {
    player.sendMessage("You clicked the left input slot!");
});        
```      

#### `itemRight(ItemStack)`
Takes a custom `ItemStack` to be placed in the right input slot.
```java                                              
ItemStack stack = new ItemStack(Material.IRON_INGOT);
ItemMeta meta = stack.getItemMeta();                 
meta.setLore(Arrays.asList("A piece of metal"));             
stack.setItemMeta(meta); 
builder.itemRight(stack);        
```         

#### `onRightInputClick(Consumer<Player>)`
Takes a `Consumer<Player>` to be executed when the item in the right input slot is clicked.
```java                                              
builder.onRightInputClick(player -> {
    player.sendMessage("You clicked the right input slot!");
});        
```

#### `title(String)`
Takes a `String` that will be used as the inventory title. Only displayed in Minecraft 1.14 and above.
```java                            
builder.title("Enter your answer");
```                                
                 
#### `plugin(Plugin)`
Takes the `Plugin` object that is making this anvil gui. It is needed to register listeners.
```java                                         
builder.plugin(pluginInstance);                 
```                            

#### `open(Player)`
Takes a `Player` that the anvil gui should be opened for. This method can be called multiple times without needing to create
a new `AnvilGUI.Builder` object.                                                                                            
```java              
builder.open(player);
```                  

### A full example combining all methods
```java
new AnvilGUI.Builder()
    .onClose(player -> {                                               //called when the inventory is closing
        player.sendMessage("You closed the inventory.");
    })
    .onComplete((player, text) -> {                                    //called when the inventory output slot is clicked
        if(text.equalsIgnoreCase("you")) {
            player.sendMessage("You have magical powers!");
            return AnvilGUI.Response.close();
        } else {
            return AnvilGUI.Response.text("Incorrect.");
        }
    })
    .preventClose()                                                    //prevents the inventory from being closed
    .text("What is the meaning of life?")                              //sets the text the GUI should start with
    .itemLeft(new ItemStack(Material.IRON_SWORD))                      //use a custom item for the first slot
    .itemRight(new ItemStack(Material.IRON_SWORD))                     //use a custom item for the second slot
    .onLeftInputClick(player -> player.sendMessage("first sword"))     //called when the left input slot is clicked
    .onRightInputClick(player -> player.sendMessage("second sword"))   //called when the right input slot is clicked
    .title("Enter your answer.")                                       //set the title of the GUI (only works in 1.14+)
    .plugin(myPluginInstance)                                          //set the plugin instance
    .open(myPlayer);                                                   //opens the GUI for the player provided
```
                                                                                                                                                                                                                                                                              

## Compilation
Build with `mvn clean install` using Java 16.

## License
This project is licensed under the [MIT License](LICENSE).
